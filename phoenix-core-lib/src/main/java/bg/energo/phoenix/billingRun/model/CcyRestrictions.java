package bg.energo.phoenix.billingRun.model;

import java.math.BigDecimal;

public interface CcyRestrictions {
    BigDecimal getValueFrom();
    BigDecimal getValueTo();
    Long getCurrencyId();
}
