package bg.energo.phoenix.billingRun.model;

import java.math.BigDecimal;

public interface EqualMonthlyData {
    BigDecimal getInstallmentAmount();
    Integer getInstallmentNumber();
    Long getCurrencyId();
    Long getAltCurrencyId();
    BigDecimal getAltExchangeRate();
}
