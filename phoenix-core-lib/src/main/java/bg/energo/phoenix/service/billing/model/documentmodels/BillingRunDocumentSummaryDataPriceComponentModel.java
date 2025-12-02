package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BillingRunDocumentSummaryDataPriceComponentModel {
    public String PC;
    public BigDecimal TotalVolumes;
    public String TotalVolumesUnit;
    public BigDecimal Price;
    public BigDecimal PriceOtherCurrency;
    public String PriceUnit;
    public BigDecimal Value;
    public String ValueUnit;
}
