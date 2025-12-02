package bg.energo.phoenix.billingRun.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CcyRestrictionsImpl {
    private BigDecimal valueFrom;
    private BigDecimal valueTo;
    private Long currencyId;

    public CcyRestrictionsImpl(BigDecimal valueFrom, BigDecimal valueTo, Long currencyId) {
        this.valueFrom = valueFrom;
        this.valueTo = valueTo;
        this.currencyId = currencyId;
    }
}
