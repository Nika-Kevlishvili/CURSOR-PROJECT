package bg.energo.phoenix.model.response.receivable.customerReceivable;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerReceivableMiddleResponse {

    Long getId();

    String getReceivableNumber();

    String getCustomer();

    String getAlternativeRecipient();

    BigDecimal getInitialAmount();

    BigDecimal getCurrentAmount();

    String getBillingGroup();

    EntityStatus getStatus();

    AccountingPeriodStatus getAccountingPeriodStatus();

    CreationType getCreationType();

    LocalDate getOccurrenceDate();

    LocalDate getDueDate();

}
