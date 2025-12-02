package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BillingRunScaleSplitBean extends Value implements Cloneable {
    private Long bdbsSplitIoId;
    private Long pcId;
    private Long pcGroupId;
    private Long podId;
    private Long podDetailId;
    private Long contractDetailId;
    private BigDecimal totalVolumesKwh;
    private BigDecimal volumes;
    private Long contractId;
    private Long runContractId;
    private Integer days;
    private Integer totalDays;
    private LocalDate dateTo;
    private Boolean isTariff;
    private String priceFormula;
    private BigDecimal runTotalPrice;
    private BigDecimal runTotalValue;
    private BigDecimal runKwhPrice;
    private Long bgInvoiceSlotId;
    private Long bdMappingId;
    private Long customerId;
    private Long customerDetailId;
    private Long productDetailId;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private BigDecimal difference;
    private BigDecimal multiplier;
    private BigDecimal correction;
    private BigDecimal deducted;
    private Long meterId;

    @Override
    public BillingRunScaleSplitBean clone() {
        try {
            BillingRunScaleSplitBean clone = (BillingRunScaleSplitBean) super.clone();
            // Since BigDecimal and LocalDate are immutable, no deep copy needed
            clone.totalVolumesKwh = totalVolumesKwh;
            clone.volumes = volumes;
            clone.dateTo = dateTo;
            clone.runTotalPrice = runTotalPrice;
            clone.runTotalValue = runTotalValue;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Should never happen
        }
    }
}
