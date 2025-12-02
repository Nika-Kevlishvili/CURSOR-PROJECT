package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDataRowParametersResponse {
    private String priceComponentOrPriceComponentGroupOrItem;
    private BigDecimal totalVolumes;
    private String unitOfMeasuresForTotalVolumes;
    private BigDecimal unitPrice;
    private String unitOfMeasureForUnitPrice;
    private BigDecimal value;
    private ShortResponse currency;
    private String incomeAccount;
    private String costCenter;
    private VatRateResponse vatRate;
    private Boolean globalVatRate;
}
