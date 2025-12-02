package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.request.contract.product.ContractPodRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractPointOfDeliveryRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractBillingLockValidationService {
    private final ContractPodRepository contractPodRepository;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final PermissionService permissionService;

    public void LockedContractValidationOnEditContract(Long productContractId,
                                                       ProductContractUpdateRequest updateRequest,
                                                       ProductContractDetails productContractDetails,
                                                       List<String> messages) {

        boolean hasEditLockedPermission = checkUserPermission();

        if (updateRequest.isSavingAsNewVersion() ||
                (updateRequest.getBasicParameters().getVersionStatus() == ProductContractVersionStatus.SIGNED
                        && productContractDetails.getVersionStatus() != ProductContractVersionStatus.SIGNED)) {

            validateNewVersionContract(
                    productContractId,
                    updateRequest,
                    productContractDetails,
                    hasEditLockedPermission,
                    messages
            );
        } else {
            validateExistingContractVersion(
                    productContractId,
                    productContractDetails,
                    updateRequest.getPodRequests(),
                    hasEditLockedPermission,
                    updateRequest.getStartDate(),
                    messages
            );
        }
    }


    private boolean checkUserPermission() {
        return permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_LOCKED)
        );
    }

    private boolean checkAdditionalAgreementPermission() {
        return permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS)
        );
    }

    public Boolean isRestrictedForAdditionalAgreementPermission(Long productContractId,
                                                                LocalDate versionDate,
                                                                Boolean hasAdditionalAgreementPermission) {
        LocalDate editRestrictionMaxDate = getContractLockDate(productContractId);
        if (editRestrictionMaxDate == null) {
            return false;
        } else {
            if (!hasAdditionalAgreementPermission) {
//                throw new OperationNotAllowedException("Access denied");
                return true;
            }
            return isEditingRestrictedByMaxDate(
                    versionDate,
                    editRestrictionMaxDate
            );
        }
    }


    private void validateExistingContractVersion(Long productContractId,
                                                 ProductContractDetails productContractDetails,
                                                 List<ContractPodRequest> podRequests,
                                                 boolean hasEditLockedPermission,
                                                 LocalDate requestedStartDate,
                                                 List<String> messages) {

        if (isRestricted(productContractId, productContractDetails.getStartDate(), hasEditLockedPermission)) {
            if (getContractLockDate(productContractId) != null
                    && !productContractDetails.getStartDate().isAfter(getContractLockDate(productContractId))
                    && !productContractDetails.getStartDate().isEqual(requestedStartDate)) {
                messages.add("Start date should not changed;");
            }
            checkPodConfigurationForEditingContract(
                    productContractDetails.getId(),
                    podRequests,
                    messages
            );
        }
    }

    private void validateNewVersionContract(Long productContractId,
                                            ProductContractUpdateRequest updateRequest,
                                            ProductContractDetails productContractDetails,
                                            boolean hasEditLockedPermission,
                                            List<String> messages) {

        if (!isRestricted(productContractId, updateRequest.getStartDate(), hasEditLockedPermission)) {
            return;
        }

        Optional<ProductContractDetails> previousVersion = productContractDetailsRepository
                .findPreviousProductContractDetailsDependingOnStartDate(
                        productContractId,
                        updateRequest.getStartDate(),
                        PageRequest.of(0, 1)
                );

        if (previousVersion.isEmpty()) {
            messages.add("Previous product contract details could not be found for the provided start date;");
        } else {
            checkPodConfigurationForEditingContract(
                    previousVersion.get().getId(),
                    updateRequest.getPodRequests(),
                    messages
            );
        }
    }

    public Boolean isRestrictedPod(Long productContractId,
                                   LocalDate currentActivationDate,
                                   LocalDate currentDeactivationDate,
                                   LocalDate requestActivationDate,
                                   LocalDate requestDeactivationDate,
                                   Boolean hasEditLockedPermission) {

        if (Boolean.FALSE.equals(hasEditLockedPermission)) {
            throw new OperationNotAllowedException("Access denied");
        }
        LocalDate editRestrictionMaxDate = getContractLockDate(productContractId);
        if (editRestrictionMaxDate == null) {
            return false;
        } else {
            boolean result = !Objects.equals(currentActivationDate, requestActivationDate) &&
                    ((Objects.nonNull(currentActivationDate) && currentActivationDate.isBefore(editRestrictionMaxDate)) ||
                            (Objects.nonNull(requestActivationDate) && requestActivationDate.isBefore(editRestrictionMaxDate)));

            result = result || (!Objects.equals(currentDeactivationDate, requestDeactivationDate) &&
                    ((Objects.nonNull(currentDeactivationDate) && currentDeactivationDate.isBefore(editRestrictionMaxDate)) ||
                            (Objects.nonNull(requestDeactivationDate) && requestDeactivationDate.isBefore(editRestrictionMaxDate))));

            return result;
        }
    }


    public Boolean isRestricted(Long productContractId,
                                LocalDate versionDate,
                                Boolean hasEditLockedPermission) {
        LocalDate editRestrictionMaxDate = getContractLockDate(productContractId);
        if (editRestrictionMaxDate == null) {
            return false;
        } else {
            if (!hasEditLockedPermission) {
                throw new OperationNotAllowedException("Access denied");
            }
            return isEditingRestrictedByMaxDate(
                    versionDate,
                    editRestrictionMaxDate
            );
        }
    }

    public Boolean isRestrictedForAutomations(Long productContractId,
                                              LocalDate versionDate,
                                              Boolean hasEditLockedPermission) {
        LocalDate editRestrictionMaxDate = getContractLockDate(productContractId);
        if (editRestrictionMaxDate == null) {
            return false;
        } else {
            if (!hasEditLockedPermission) {
                return true;
            }
            return isEditingRestrictedByMaxDate(
                    versionDate,
                    editRestrictionMaxDate
            );
        }
    }

    public Boolean isRestrictedForAutomationsPod(Long productContractId,
                                                 LocalDate deactivationDate,
                                                 LocalDate currentDeactivationDate,
                                                 Boolean hasEditLockedPermission) {
        LocalDate editRestrictionMaxDate = getContractLockDate(productContractId);
        if (editRestrictionMaxDate == null) {
            return false;
        } else {
            if (!hasEditLockedPermission) {
                return true;
            }
            if (Objects.nonNull(currentDeactivationDate) && currentDeactivationDate.isBefore(editRestrictionMaxDate)) {
                return !Objects.equals(deactivationDate, currentDeactivationDate);
            } else {
                return !deactivationDate.isAfter(editRestrictionMaxDate);
            }
        }
    }

    public Boolean isEditingRestrictedByMaxDate(LocalDate contractVersionDate,
                                                LocalDate editRestrictionMaxDate) {
        return editRestrictionMaxDate != null
                && !contractVersionDate.isAfter(editRestrictionMaxDate);
    }

    public LocalDate getContractLockDate(Long productContractId) {
        return productContractRepository.getContractLockDate(productContractId);
    }

    private void checkPodConfigurationForEditingContract(Long productContractDetailId,
                                                         List<ContractPodRequest> podRequestList,
                                                         List<String> errorMessages) {
        List<ContractPods> existingPods = contractPodRepository.findAllByContractDetailIdAndStatusIn(
                productContractDetailId,
                listOf(EntityStatus.ACTIVE));

        Map<Long, ContractPods> existingPodsMap = existingPods.stream()
                .collect(Collectors.toMap(ContractPods::getPodDetailId, pod -> pod));

        int totalPointOfDeliveriesFromRequest = podRequestList
                .stream()
                .mapToInt(podRequest ->
                        podRequest
                                .getProductContractPointOfDeliveries()
                                .size())
                .sum();

        if (totalPointOfDeliveriesFromRequest != existingPods.size()) {
            errorMessages.add("The total number of delivery points in the request ("
                    + totalPointOfDeliveriesFromRequest
                    + ") does not match the number of delivery points in the existing contract ("
                    + existingPods.size()
                    + ");"
            );
        }

        for (ContractPodRequest podRequest : podRequestList) {
            for (ProductContractPointOfDeliveryRequest podRequestDetail : podRequest.getProductContractPointOfDeliveries()) {

                ContractPods correspondingPod = existingPodsMap.remove(podRequestDetail.pointOfDeliveryDetailId());

                if (correspondingPod == null) {
                    errorMessages.add(
                            "Pod with pointOfDeliveryDetailId "
                                    + podRequestDetail.pointOfDeliveryDetailId()
                                    + " in request does not exist in the current contract;"
                    );
                    continue;
                }

                if (!Objects.equals(correspondingPod.getBillingGroupId(), podRequest.getBillingGroupId())) {
                    errorMessages.add(
                            "Mismatch in billingGroupId for Pod with pointOfDeliveryDetailId: "
                                    + podRequestDetail.pointOfDeliveryDetailId()
                                    + ";"
                    );
                }
                if (!Objects.equals(correspondingPod.getDealNumber(), podRequestDetail.dealNumber())) {
                    errorMessages.add(
                            "Mismatch in dealNumber for Pod with pointOfDeliveryDetailId: "
                                    + podRequestDetail.pointOfDeliveryDetailId()
                                    + ";"
                    );
                }
            }
        }

        if (!existingPodsMap.isEmpty()) {
            existingPodsMap.keySet().forEach(remainingPodId ->
                    errorMessages.add(
                            "Pod with pointOfDeliveryDetailId "
                                    + remainingPodId
                                    + " exists in the current contract but not in the request;"
                    )
            );
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(
                errorMessages,
                log
        );

    }

}
