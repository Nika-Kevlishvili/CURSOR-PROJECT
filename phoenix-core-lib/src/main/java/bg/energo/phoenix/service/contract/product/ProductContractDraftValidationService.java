package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.request.contract.product.ContractPodRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractPointOfDeliveryRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus.READY;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractDraftValidationService {
    private final ContractPodRepository contractPodRepository;
    private final ProductContractRepository productContractRepository;
    private final PermissionService permissionService;
    private final ProductDetailsRepository productDetailsRepository;
    private final CustomerDetailsRepository customerDetailsRepository;


    public void draftContractValidationForAdditionalAgreementUser(Long productContractId,
                                                                  ProductContractUpdateRequest updateRequest,
                                                                  ProductContractDetails productContractDetails,
                                                                  List<String> messages) {

        boolean hasAdditionalAgreementPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS)
        );
        boolean hasEditLockedPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_LOCKED)
        );
        boolean hasEditPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT)
        );
        if (hasAdditionalAgreementPermission && !hasEditLockedPermission) {
            if (!hasEditPermission && !updateRequest.isSavingAsNewVersion()) {
                throw new OperationNotAllowedException("Is not allowed change current version");
            }
            if (!hasEditPermission) {

                validateUnchangedBasicParameters(
                        productContractId,
                        updateRequest,
                        productContractDetails,
                        messages);
                validateUnchangedAdditionalParameters(
                        updateRequest,
                        productContractDetails,
                        messages);
                validateUnchangedPodParameters(
                        updateRequest,
                        productContractDetails,
                        messages
                );
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(
                messages,
                log
        );
    }


    private void validateUnchangedAdditionalParameters(
            ProductContractUpdateRequest request,
            ProductContractDetails sourceProductContractDetails,
            List<String> messages
    ) {

        if (!Objects.equals(request.getAdditionalParameters().getBankingDetails().getDirectDebit(),
                sourceProductContractDetails.getDirectDebit())) {
            messages.add("Additional Parameters: 'Direct debit' checkbox should not be changed;");
        }
        if (!Objects.equals(request.getAdditionalParameters().getInterestRateId(),
                sourceProductContractDetails.getApplicableInterestRate())) {
            messages.add("Additional Parameters: 'Applicable interest rate' drop-down should not be changed;");
        }
        if (!Objects.equals(request.getAdditionalParameters().getBankingDetails().getBankId(),
                sourceProductContractDetails.getBankId())) {
            messages.add("Additional Parameters: 'Bank' field should not be changed;");
        }
        if (!Objects.equals(request.getAdditionalParameters().getBankingDetails().getIban(),
                sourceProductContractDetails.getIban())
        ) {
            messages.add("Additional Parameters: 'IBAN' field should not be changed;");
        }
    }

    private void validateUnchangedBasicParameters(
            Long productContractId,
            ProductContractUpdateRequest request,
            ProductContractDetails sourceProductContractDetails,
            List<String> messages
    ) {

        ProductContract contract = findProductContract(productContractId);
        if (request.getBasicParameters().getVersionStatus() != READY) {
            messages.add("Version status should be READY;");

        }
        if (request.getBasicParameters().getType() != ContractDetailType.ADDITIONAL_AGREEMENT) {
            messages.add("Type should be ADDITIONAL_AGREEMENT;");
        }
        if (!Objects.equals(request.getBasicParameters().getTerminationDate(), contract.getTerminationDate())) {
            messages.add("Termination date should not be changed;");
        }

        if (!Objects.equals(request.getBasicParameters().getPerpetuityDate(), contract.getPerpetuityDate())) {
            messages.add("Perpetuity date should not be changed;");
        }

        if (!Objects.equals(request.getBasicParameters().getContractTermEndDate(), contract.getContractTermEndDate())) {
            messages.add("Term end date should not be changed;");
        }

        if (!Objects.equals(request.getBasicParameters().getStatus(), contract.getContractStatus())) {
            messages.add("Contract status should not be changed;");
        }

        if (!Objects.equals(request.getBasicParameters().getSubStatus(), contract.getSubStatus())) {
            messages.add("Contract sub status should not be changed;");
        }

//        if (!Objects.equals(request.getBasicParameters().getStatusModifyDate(), contract.getStatusModifyDate())) {
//            messages.add("Basic Parameter 'statusModifyDate' should not be changed;");
//        }

        ProductDetails sourceProduct = productDetailsRepository
                .findById(sourceProductContractDetails.getProductDetailId())
                .orElse(null);
        if (sourceProduct == null) {
            throw new OperationNotAllowedException("Is not allowed change current version");
        }

        if (!Objects.equals(request.getBasicParameters().getProductId(), sourceProduct.getProduct().getId())) {
            messages.add("Basic Parameter 'productId' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getProductVersionId(), sourceProduct.getVersion())) {
            messages.add("Basic Parameter 'productVersionId' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().isHasUntilAmount(), sourceProductContractDetails.getContractTermUntilTheAmount())) {
            messages.add("Basic Parameter 'hasUntilAmount' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().isHasUntilVolume(), sourceProductContractDetails.getContractTermUntilTheVolume())) {
            messages.add("Basic Parameter 'hasUntilVolume' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().isProcurementLaw(), sourceProductContractDetails.getPublicProcurementLaw())) {
            messages.add("Basic Parameter 'procurementLaw' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getUntilAmount(), sourceProductContractDetails.getContractTermUntilTheAmountValue())) {
            messages.add("Basic Parameter 'untilAmount' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getUntilVolume(), sourceProductContractDetails.getContractTermUntilTheVolumeValue())) {
            messages.add("Basic Parameter 'untilVolume' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getUntilAmountCurrencyId(), sourceProductContractDetails.getCurrencyId())) {
            messages.add("Basic Parameter 'untilAmountCurrencyId' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getSigningDate(), contract.getSigningDate())) {
            messages.add("Basic Parameter 'signingDate' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getEntryInForceDate(), sourceProductContractDetails.getEntryIntoForceDate())) {
            messages.add("Basic Parameter 'entryInForceDate' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getStartOfInitialTerm(), sourceProductContractDetails.getInitialTermDate())) {
            messages.add("Basic Parameter 'startOfInitialTerm' should not be changed;");
        }

        CustomerDetails sourceCustomer = customerDetailsRepository
                .findById(sourceProductContractDetails.getCustomerDetailId())
                .orElse(null);
        if (sourceCustomer == null) {
            throw new OperationNotAllowedException("Is not allowed change current version");
        }

        if (!Objects.equals(request.getBasicParameters().getCustomerId(), sourceCustomer.getCustomerId())) {
            messages.add("Basic Parameter 'customerId' should not be changed;");
        }
        if (!Objects.equals(request.getBasicParameters().getCustomerVersionId(), sourceCustomer.getVersionId())) {
            messages.add("Basic Parameter 'customerVersionId' should not be changed;");
        }
    }

    private void validateUnchangedPodParameters(
            ProductContractUpdateRequest request,
            ProductContractDetails sourceProductContractDetails,
            List<String> messages
    ) {
        List<ContractPods> existingPods = contractPodRepository.findAllByContractDetailIdAndStatusIn(
                sourceProductContractDetails.getId(),
                listOf(EntityStatus.ACTIVE));

        Map<Long, ContractPods> existingPodsMap = existingPods.stream()
                .collect(Collectors.toMap(ContractPods::getPodDetailId, pod -> pod));

        for (ContractPodRequest podRequest : request.getPodRequests()) {
            for (ProductContractPointOfDeliveryRequest podRequestDetail : podRequest.getProductContractPointOfDeliveries()) {

                ContractPods correspondingPod = existingPodsMap.remove(podRequestDetail.pointOfDeliveryDetailId());
                if (correspondingPod == null && podRequestDetail.dealNumber() != null) {
                    messages.add("Deal number should not be added for a new Pod (pointOfDeliveryDetailId: "
                            + podRequestDetail.pointOfDeliveryDetailId() + ")");
                    continue;
                }

                if (correspondingPod != null && !Objects.equals(correspondingPod.getDealNumber(), podRequestDetail.dealNumber())) {
                    messages.add(
                            "Mismatch in dealNumber for Pod with pointOfDeliveryDetailId: "
                                    + podRequestDetail.pointOfDeliveryDetailId()
                    );
                }
            }
        }
    }

    private ProductContract findProductContract(Long productContractId) {
        ProductContract contract = productContractRepository
                .findById(productContractId)
                .orElse(null);
        if (contract == null) {
            throw new OperationNotAllowedException("Is not allowed to change current version");
        }
        return contract;
    }

}
