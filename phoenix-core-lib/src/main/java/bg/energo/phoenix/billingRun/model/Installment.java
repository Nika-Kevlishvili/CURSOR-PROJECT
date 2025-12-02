package bg.energo.phoenix.billingRun.model;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class Installment {
    private Long currencyId;
    private BigDecimal amount;
    private Boolean equalized;
    private Boolean isEqualization;
    private Boolean generateEqualMonthly;
}
