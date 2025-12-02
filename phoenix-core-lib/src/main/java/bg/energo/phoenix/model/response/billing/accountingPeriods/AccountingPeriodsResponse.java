package bg.energo.phoenix.model.response.billing.accountingPeriods;

import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingPeriodsResponse {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private AccountingPeriodStatus status;
    private LocalDateTime modifyDate;

    public AccountingPeriodsResponse(AccountingPeriods accountingPeriods) {
        this.id = accountingPeriods.getId();
        this.name = accountingPeriods.getName();
        this.startDate = accountingPeriods.getStartDate();
        this.endDate = accountingPeriods.getEndDate();
        this.status = accountingPeriods.getStatus();
        this.modifyDate = accountingPeriods.getModifyDate();
    }
}
