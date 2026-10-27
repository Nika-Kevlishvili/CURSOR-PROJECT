package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualPosBillingGroupHelperService {

    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;

    /**
     * Resolves the contract billing group the payment must be booked under.
     * <p>The group is looked up from the billing group number the payer supplied in the IDN, because
     * that is what the liability snapshot was selected against. Deriving it from an arbitrary
     * liability of the snapshot is only a fallback: {@code paymentDetails} has no guaranteed order,
     * so for a multi-liability payment it can yield the group of an unrelated liability.
     */
    public Long fetchContractBillingGroupIdForCustomer(
            Long customerId,
            String billingGroupNumber,
            List<VirtualPosPaymentDetailsEntity> paymentDetails
    ) {
        if (StringUtils.isBlank(billingGroupNumber)) {
            return null;
        }

        final Long contractBillingGroupId = contractBillingGroupRepository
                .findByCustomerIdAndGroupNumber(customerId, billingGroupNumber)
                .map(CacheObject::getId)
                .orElse(null);

        if (Objects.isNull(contractBillingGroupId)) {
            Long fallbackBillingGroupId = firstContractBillingGroupIdOf(paymentDetails);
            log.warn(
                    "No active contract billing group [{}] found for customerId: {}. Falling back to the group of the paid liabilities: {}",
                    billingGroupNumber,
                    customerId,
                    fallbackBillingGroupId
            );
            return fallbackBillingGroupId;
        }

        List<Long> foreignLiabilityIds = paymentDetails
                .stream()
                .map(VirtualPosPaymentDetailsEntity::getLiabilityId)
                .filter(Objects::nonNull)
                .filter(liabilityId -> !contractBillingGroupId.equals(fetchContractBillingGroupId(liabilityId)))
                .toList();

        if (!foreignLiabilityIds.isEmpty()) {
            log.warn(
                    "Liabilities {} do not belong to contract billing group [{}] (id: {}) selected by customerId: {}. Offsetting may be rejected.",
                    foreignLiabilityIds,
                    billingGroupNumber,
                    contractBillingGroupId,
                    customerId
            );
        }

        return contractBillingGroupId;
    }

    private Long firstContractBillingGroupIdOf(List<VirtualPosPaymentDetailsEntity> paymentDetails) {
        return paymentDetails
                .stream()
                .map(VirtualPosPaymentDetailsEntity::getLiabilityId)
                .filter(Objects::nonNull)
                .map(this::fetchContractBillingGroupId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public Long fetchAccountPeriodId() {
        return accountingPeriodsRepository
                .findAccountingPeriodsByDate(LocalDateTime.now())
                .map(CacheObject::getId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period not found!;"));
    }

    private Long fetchContractBillingGroupId(Long liabilityId) {
        return customerLiabilityRepository
                .findByIdAndStatus(liabilityId, EntityStatus.ACTIVE)
                .map(CustomerLiability::getContractBillingGroupId)
                .orElse(null);
    }
}


