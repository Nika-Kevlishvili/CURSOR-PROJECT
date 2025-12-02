package bg.energo.phoenix.billingRun.model;

import java.math.BigDecimal;

public interface StandardInvoiceForInterim {
    BigDecimal getPrice();
    BigDecimal getPercent();
    Long getMainCurrencyId();
}
