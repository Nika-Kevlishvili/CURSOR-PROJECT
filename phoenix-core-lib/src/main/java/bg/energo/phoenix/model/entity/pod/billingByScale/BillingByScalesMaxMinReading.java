package bg.energo.phoenix.model.entity.pod.billingByScale;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BillingByScalesMaxMinReading {
    private BigDecimal maxReading;
    private BigDecimal minReading;

    public BillingByScalesMaxMinReading(BigDecimal maxReading, BigDecimal minReading) {
        this.maxReading = maxReading;
        this.minReading = minReading;
    }
}
