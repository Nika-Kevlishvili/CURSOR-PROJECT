package bg.energo.phoenix.service.contract.newVersionEvent;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractDataModificationResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractThirdPageFields;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.service.contract.billing.BillingGroupService;
import bg.energo.phoenix.service.contract.product.*;
import bg.energo.phoenix.service.customer.CustomerMapperService;
import bg.energo.phoenix.service.product.product.ProductRelatedContractUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractNewVersionEventHandler {
    private final ProductRelatedContractUpdateService productRelatedContractUpdateService;
    private final ProductContractService productContractService;
    private final CustomerMapperService customerMapperService;
    private final ProductContractDetailsRepository productContractDetailsRepository;

    private final ProductContractBasicParametersService basicParametersService;
    private final ProductContractAdditionalParametersService additionalParametersService;
    private final ProductContractPodService productContractPodService;

    private final ProductContractRepository productContractRepository;
    private final ProductContractProductParametersService productParametersService;
    private final BillingGroupService billingGroupService;


    @Async
    @Transactional
    @EventListener(ProductContractCreateNewVersionEvent.class)
    public void handleEvent(ProductContractCreateNewVersionEvent event) {
        SecurityContextHolder.setContext(event.getContext());
        log.info("Start creating new product contract version");
        //create new version of product Contract and fill with new product options
        ProductContractResponse productContractResponse = getProductContractResponse(
                event.getProductRelatedContractId(),
                event.getProductRelatedContractVersion()
        );
        ProductContractUpdateRequest productContractUpdateRequest = customerMapperService
                .mapProductContractUpdateRequest(
                        productContractResponse,
                        LocalDate.now(),
                        event.getProductRelatedContractCustomerDetailId()
                );
        ProductContractDataModificationResponse updatedProduct = productContractService.edit(
                event.getProductRelatedContractId(),
                event.getProductRelatedContractVersion(),
                productContractUpdateRequest,
                false
        );
        Integer maxVersionId = productContractDetailsRepository.findMaxVersionId(updatedProduct.id());
        Optional<ProductContractDetails> updatedProdContrOptional = productContractDetailsRepository
                .findByProductContractIdAndVersionId(
                        updatedProduct.id(),
                        maxVersionId
                );
        if (updatedProdContrOptional.isPresent()) {
            ProductContractDetails updatedProdContr = updatedProdContrOptional.get();
            productRelatedContractUpdateService.fillProductContractDetail(
                    updatedProdContr,
                    event.getCurrentProductDetails(),
                    event.getExceptionMessagesContext(),
                    event.getProductContractValidTerm(),
                    event.getProductDetail()
            );
        }
    }

    public ProductContractResponse getProductContractResponse(Long id, Integer versionId) {
        log.debug("Retrieving product contract details for contract id: {}, version id: {}", id, versionId);
        ProductContractResponse response = new ProductContractResponse();
        ProductContract productContract = productContractRepository
                .findByIdAndStatusIn(id, List.of(ProductContractStatus.ACTIVE, ProductContractStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product contract not found!"));
        response.setStatus(productContract.getStatus());
        response.setLocked(productContract.getLocked() == null ? Boolean.FALSE : productContract.getLocked());
        ProductContractDetails details;
        if (versionId != null) {
            details = productContractDetailsRepository
                    .findByContractIdAndVersionId(id, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product contract details not found"));
        } else {
            details = productContractDetailsRepository
                    .findFirstByContractIdOrderByStartDateDesc(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product contract details not found"));
        }

        // additional parameters tab full response
        response.setBasicParameters(basicParametersService.getBasicParameterResponse(productContract, details));
        response.setAdditionalParameters(additionalParametersService.getAdditionalParametersResponse(details));
        ProductContractThirdPageFields thirdPageTabs = productParametersService
                .thirdTabFields(details.getProductDetailId());
        response.setThirdPageTabs(thirdPageTabs);
        response.setProductParameters(productParametersService.thirdPagePreview(
                details,
                productContract,
                thirdPageTabs)
        );
        response.setContractPodsResponses(productContractPodService.getAllPodsByCustomerDetailId(details.getId()));
        response.setBillingGroups(billingGroupService.findContractBillingGroups(productContract.getId()));
        response.setVersions(productContractDetailsRepository.findProductContractVersionsOrderedByStatusAndStartDate(
                productContract.getId()));
        return response;
    }

}
