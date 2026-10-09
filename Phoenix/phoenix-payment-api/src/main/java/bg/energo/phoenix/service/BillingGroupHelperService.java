package bg.energo.phoenix.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EasyPayPaymentDetailsEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
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
public class BillingGroupHelperService {

    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;

    /**
     * Retrieves the contract billing group ID for a given customer.
     * This method first checks if the customer has a non-blank billing group number.
     * If the billing group number is provided, it attempts to fetch the corresponding
     * {@link EasyPayPaymentDetailsEntity} from a collection of entities, retrieves its
     * liability ID, and uses it to look up the related contract billing group ID.
     * If the contract billing group ID is not found, it returns {@code null}.
     *
     * @param easyPayPaymentDetailsEntities A collection of {@link EasyPayPaymentDetailsEntity} objects that
     *                                      contain payment details related to the customer.
     * @return The contract billing group ID if found; {@code null} if not found or if the billing group number
     * is blank or invalid.
     */
    public Long fetchContractBillingGroupIdForCustomer(
            String billingGroupNumber,
            List<EasyPayPaymentDetailsEntity> easyPayPaymentDetailsEntities
    ) {
        Long liabilityId = StringUtils.isNotBlank(billingGroupNumber) ? easyPayPaymentDetailsEntities
                .stream()
                .findFirst()
                .map(EasyPayPaymentDetailsEntity::getLiabilityId)
                .orElse(null)
                : null;

        Long contractBillingGroupId = null;

        if (Objects.nonNull(billingGroupNumber)) {
            contractBillingGroupId = fetchContractBillingGroupId(liabilityId);
            if (Objects.isNull(contractBillingGroupId)) {
                return null;
            }
        }

        return contractBillingGroupId;
    }

    /**
     * Retrieves the ID of the current accounting period based on the system's current date and time.
     * <p>
     * This method queries the {@code AccountingPeriodsRepository} to find an accounting period
     * corresponding to the current {@code LocalDateTime}. If an accounting period is found, its ID
     * is returned. If no accounting period exists for the current date and time,
     * a {@link DomainEntityNotFoundException} is thrown.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Long accountingPeriodId = fetchAccountPeriodId();
     * // Use the accountingPeriodId for further operations
     * }</pre>
     *
     * @return the ID of the accounting period valid for the current date and time.
     * @throws DomainEntityNotFoundException if no accounting period is found for the current date and time.
     */
    public Long fetchAccountPeriodId(LocalDateTime paymentDate) {
        return accountingPeriodsRepository
                .findAccountingPeriodsByDate(paymentDate)
                .map(CacheObject::getId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period not found!;"));
    }

    /**
     * Fetches the contract billing group ID associated with the given liability ID.
     * <p>
     * This method queries the {@link CustomerLiabilityRepository} to find a {@link CustomerLiability}
     * with the specified liability ID and an active status. If a matching record is found, it returns
     * the contract billing group ID. If no record is found or the status is not active, it returns {@code null}.
     *
     * @param liabilityId the ID of the liability for which to fetch the contract billing group ID
     * @return the contract billing group ID if found, or {@code null} if no active contract liability
     * with the specified ID exists
     */
    private Long fetchContractBillingGroupId(Long liabilityId) {
        return customerLiabilityRepository
                .findByIdAndStatus(liabilityId, EntityStatus.ACTIVE)
                .map(CustomerLiability::getContractBillingGroupId)
                .orElse(null);
    }

}
