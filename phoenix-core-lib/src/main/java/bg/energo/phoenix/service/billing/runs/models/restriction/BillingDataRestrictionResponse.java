package bg.energo.phoenix.service.billing.runs.models.restriction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingDataRestrictionResponse {
    //TODO remove unnecessary fields
    //values from billing restriction model
    private Long modelId;
    private Integer invoiceId;
    private Long podId;
    private BigDecimal totalVolumes;
    private BigDecimal price;
    private LocalDate calculatedFrom;
    private LocalDate calculatedTo;
    private Long priceComponentId;
    private Long currencyId;
    // restricted values
    private BigDecimal volumesOfPercentageRestriction;
    private BigDecimal amountOfPercentageRestriction;
    private BigDecimal volumesOfKwhRestriction;
    private BigDecimal amountOfKwhRestriction;
    private BigDecimal volumesOfCcyRestriction;
    private BigDecimal amountOfCcyRestriction;
    // calculated total amount for model
    private BigDecimal totalAmount;
    // calculated total percent restriction for whole priceComponent
    private BigDecimal totalPercentRestrictionByPriceComponent;
    //final prioritized restriction values
    private BigDecimal finalRestrictionVolume;
    private BigDecimal finalRestrictionAmount;
}
