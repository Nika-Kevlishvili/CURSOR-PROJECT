package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
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

    public Long fetchContractBillingGroupIdForCustomer(
            String billingGroupNumber,
            List<VirtualPosPaymentDetailsEntity> paymentDetails
    ) {
        Long liabilityId = StringUtils.isNotBlank(billingGroupNumber) ? paymentDetails
                .stream()
                .findFirst()
                .map(VirtualPosPaymentDetailsEntity::getLiabilityId)
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


