package bg.energo.phoenix.service.billing.runs.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BillingDataCalculatedDiscountModel {
    private Long modelId;
    private BigDecimal totalVolume;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private BigDecimal discountAmount;
    private BigDecimal discountPerKWH;
}
