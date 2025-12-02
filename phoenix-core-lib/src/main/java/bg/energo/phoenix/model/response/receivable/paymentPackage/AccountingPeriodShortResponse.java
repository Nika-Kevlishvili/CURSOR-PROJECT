package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;

public record AccountingPeriodShortResponse(
        Long id,
        String name
) {
    public AccountingPeriodShortResponse(AccountingPeriods accountingPeriod) {
        this(accountingPeriod.getId(), accountingPeriod.getName());
    }
}
