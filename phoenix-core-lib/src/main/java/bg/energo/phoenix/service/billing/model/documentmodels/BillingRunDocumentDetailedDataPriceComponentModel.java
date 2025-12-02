package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class BillingRunDocumentDetailedDataPriceComponentModel {
    public String PC;
    public String ScaleCode;
    public BigDecimal NewMR;
    public BigDecimal OldMR;
    public BigDecimal DifferenceM;
    public BigDecimal Multiplier;
    public BigDecimal Correction;
    public BigDecimal Deducted;
    public BigDecimal Value;
    public String ValueUnit;
    public BigDecimal Price;
    public BigDecimal PriceOtherCurrency;
    public String PriceUnit;
    public String PriceUnitOtherCurrency;
    public BigDecimal TotalVolumes;
    public String TotalVolumesUnit;
}
