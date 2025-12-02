package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public class BillingRunDocumentDetailedDataPriceComponentProfileModel {
    public String PC;
    public LocalDate PeriodFrom;
    public LocalDate PeriodTo;
    public BigDecimal Value;
    public String ValueUnit;
    public BigDecimal Price;
    public BigDecimal PriceOtherCurrency;
    public String PriceUnit;
    public String PriceUnitOtherCurrency;
    public BigDecimal TotalVolumes;
    public String TotalVolumesUnit;
}
