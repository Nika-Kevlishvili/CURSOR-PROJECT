package bg.energo.phoenix.service.billing.runs.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillingDataRestrictionModel {

    private Long modelId;
    private Integer invoiceId;
    private Long podId;
    //not sure about type
    private BigDecimal totalVolumes;
    private BigDecimal price;

    private LocalDate calculatedFrom;
    private LocalDate calculatedTo;

    private Long priceComponentId;

    private Long currencyId;
}
