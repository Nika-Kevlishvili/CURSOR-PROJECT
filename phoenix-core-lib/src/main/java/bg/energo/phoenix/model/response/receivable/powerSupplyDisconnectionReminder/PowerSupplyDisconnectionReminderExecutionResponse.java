package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import java.math.BigDecimal;

public interface PowerSupplyDisconnectionReminderExecutionResponse {

    Long getCustomerId();
    Long getLiabilityId();
    BigDecimal getCurrentAmount();
    Long getPsdrId();
    Long getCurrencyId();
    Long getAlternativeCurrencyId();
    BigDecimal getAlternativeExchangeRate();
    Long getPsdrCurrencyId();
    BigDecimal getAmountFrom();
    BigDecimal getAmountTo();
    BigDecimal getSumCurrentAmount();
}
