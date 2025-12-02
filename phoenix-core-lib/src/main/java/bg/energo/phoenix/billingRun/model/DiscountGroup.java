package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DiscountGroup {
    protected Long podId;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal volumes;
    private Long customerId;
    private Long pcGroupId;
    private Long pcId;
    private Long bgInvoiceSlotId;
    private Long contractDetailId;
    private Long customerDetailId;
    private Long priceComponentPriceTypeId;
    private Long productDetailId;

}
