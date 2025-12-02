package bg.energo.phoenix.model.response.billing.accountingPeriods;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AccountingPeriodsListingResponse {
    private Long accountPeriodId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private AccountingPeriodStatus status;
    private LocalDate closedOnDate;


    public AccountingPeriodsListingResponse(AccountingPeriodsListingMiddleResponse accountingPeriodsListingMiddleResponse) {
        this.accountPeriodId = accountingPeriodsListingMiddleResponse.getAccountPeriodId();
        this.name = accountingPeriodsListingMiddleResponse.getName();
        this.startDate = accountingPeriodsListingMiddleResponse.getStartDate();
        this.endDate = accountingPeriodsListingMiddleResponse.getEndDate();
        this.status = accountingPeriodsListingMiddleResponse.getStatus();
        this.closedOnDate = accountingPeriodsListingMiddleResponse.getClosedOnDate();
    }
}
