package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.request.product.product.BaseProductTermsRequest;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductTermsService {
    private final ProductContractTermRepository termRepository;

    @Transactional
    public void create(List<BaseProductTermsRequest> request, Long productDetailsId) {
        List<ProductContractTerms> productContractTerms = request.stream()
                .map(x -> new ProductContractTerms(x, productDetailsId)).toList();
        termRepository.saveAll(productContractTerms);
    }

    @Transactional
    public void edit(List<BaseProductTermsRequest> request, ProductDetails productDetails, List<String> exceptionMessages) {
        List<ProductContractTerms> activeProductContractTerms =
                termRepository
                        .findAllByProductDetailsIdAndStatusInOrderByCreateDate(
                                productDetails.getId(),
                                List.of(ProductSubObjectStatus.ACTIVE));

        List<Long> requestedProductTermsIds = request
                .stream()
                .map(BaseProductTermsRequest::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Long> activeProductTermIds = activeProductContractTerms
                .stream()
                .map(ProductContractTerms::getId)
                .toList();

        requestedProductTermsIds
                .stream()
                .filter(id -> !activeProductTermIds.contains(id))
                .forEach(id -> exceptionMessages.add("basicSettings.productTerms[%s].id-Term with presented id [%s] not found;".formatted(requestedProductTermsIds.indexOf(id), id)));

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        activeProductContractTerms
                .forEach(productContractTerms -> {
                    if (requestedProductTermsIds.contains(productContractTerms.getId())) {
                        productContractTerms.setStatus(ProductSubObjectStatus.ACTIVE);
                    } else {
                        productContractTerms.setStatus(ProductSubObjectStatus.DELETED);
                    }
                });

        request.stream()
                .filter(baseProductTermsRequest -> baseProductTermsRequest.getId() != null)
                .forEach(baseProductTermsRequest -> {
                    Optional<ProductContractTerms> productContractTermsOptional = activeProductContractTerms
                            .stream()
                            .filter(activeProductContractTerm ->
                                    activeProductContractTerm.getId().equals(baseProductTermsRequest.getId()))
                            .findFirst();
                    if (productContractTermsOptional.isPresent()) {
                        ProductContractTerms productContractTerms = productContractTermsOptional.get();
                        productContractTerms.setName(baseProductTermsRequest.getName());
                        productContractTerms.setPeriodType(baseProductTermsRequest.getTypeOfTerms());
                        productContractTerms.setType(baseProductTermsRequest.getPeriodType());
                        productContractTerms.setValue(baseProductTermsRequest.getValue());
                        productContractTerms.setPerpetuityCause(baseProductTermsRequest.isPerpetuityCause());
                        productContractTerms.setAutomaticRenewal(baseProductTermsRequest.getAutomaticRenewal());
                        productContractTerms.setNumberOfRenewals(baseProductTermsRequest.getNumberOfRenewals());
                        productContractTerms.setRenewalPeriodType(baseProductTermsRequest.getRenewalPeriodType());
                        productContractTerms.setRenewalPeriodValue(baseProductTermsRequest.getRenewalPeriodValue());
                    } else {
                        exceptionMessages.add("basicSettings.productTerms[%s].id-Active term with presented id [%s] not found;");
                    }
                });

        List<ProductContractTerms> newProductContractTerms = request.stream()
                .filter(productTermsEditRequest -> productTermsEditRequest.getId() == null)
                .map(productTermsEditRequest -> new ProductContractTerms(productTermsEditRequest, productDetails.getId()))
                .toList();

        termRepository.saveAll(newProductContractTerms);
        termRepository.saveAll(activeProductContractTerms);
    }
}
