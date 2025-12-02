package bg.energo.phoenix.model.response.billing.accountingPeriods;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;

import java.time.LocalDate;

public interface AccountingPeriodsListingMiddleResponse {
    Long getAccountPeriodId();

    String getName();

    LocalDate getStartDate();

    LocalDate getEndDate();

    AccountingPeriodStatus getStatus();

    LocalDate getClosedOnDate();
}
