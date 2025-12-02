package bg.energo.phoenix.model.response.receivable.customerLiability;


import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerLiabilityListingMiddleResponse {
    Long getCustomerId();
    String getLiabilityNumber();
    String getCustomer();
    String getBillingGroup();
    String getAlternativeRecipientOfAnInvoice();
    BigDecimal getInitialAmount();
    BigDecimal getCurrentAmount();
    Long getCurrencyId();
    Long getId();
    EntityStatus getStatus();
    CreationType getCreationType();
    AccountingPeriodStatus getAccountingPeriodStatus();
    String getCurrencyName();

    LocalDate getOccurrenceDate();
    LocalDate getDueDate();
    String getPods();
}
