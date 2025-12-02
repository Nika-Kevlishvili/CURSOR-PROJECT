package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailedDataRowParametersResponse {
    private String priceComponent;
    private String pointOfDelivery;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String meter;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private BigDecimal differences;
    private BigDecimal multiplier;
    private BigDecimal correction;
    private BigDecimal deducted;
    private BigDecimal totalVolumes;
    private String unitOfMeasureForTotalVolumes;
    private BigDecimal unitPrice;
    private String unitOfMeasureForUnitPrice;
    private BigDecimal currentValue;
    private ShortResponse currency;
    private String incomeAccount;
    private String costCenter;
    private VatRateResponse vatRate;
    private Boolean globalVatRate;
}
