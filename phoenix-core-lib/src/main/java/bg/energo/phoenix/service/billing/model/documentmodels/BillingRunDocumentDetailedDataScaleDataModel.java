package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public class BillingRunDocumentDetailedDataScaleDataModel {
    public String ScaleType;
    public LocalDate PeriodFrom;
    public LocalDate PeriodTo;
    public BigDecimal NewMR;
    public BigDecimal OldMR;
    public BigDecimal Difference;
    public BigDecimal Multiplier;
    public BigDecimal Correction;
    public BigDecimal Deducted;
    public BigDecimal Volumes;
}
