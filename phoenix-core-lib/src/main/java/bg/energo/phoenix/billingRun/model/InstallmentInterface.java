package bg.energo.phoenix.billingRun.model;

import java.math.BigDecimal;

public interface InstallmentInterface {
    public BigDecimal getAmount();
    public Long currencyId();
}
