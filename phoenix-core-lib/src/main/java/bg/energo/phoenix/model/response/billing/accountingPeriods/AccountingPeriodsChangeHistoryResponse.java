package bg.energo.phoenix.model.response.billing.accountingPeriods;

import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriodsStatusChangeHistory;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountingPeriodsChangeHistoryResponse {
    private Long id;
    private Long accountPeriodId;
    private AccountingPeriodStatus status;
    private LocalDateTime createDate;

    public AccountingPeriodsChangeHistoryResponse(AccountingPeriodsStatusChangeHistory accountingPeriodsStatusChangeHistory) {
        this.id = accountingPeriodsStatusChangeHistory.getId();
        this.accountPeriodId = accountingPeriodsStatusChangeHistory.getAccountingPeriodId();
        this.status = accountingPeriodsStatusChangeHistory.getStatus();
        this.createDate = accountingPeriodsStatusChangeHistory.getCreateDate();
    }
}
