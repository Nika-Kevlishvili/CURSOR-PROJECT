package bg.energo.phoenix.service.billing.model.documentmodels;

import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentVatBaseProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BillingRunExcludedPriceComponentSummaryModel{

    public String PC;
    private String DocumentNumber;
    private LocalDate DocumentDate;
    public BigDecimal TotalVolumes;
    public String TotalVolumesUnit;
    public BigDecimal Price;
    public BigDecimal PriceOtherCurrency;
    public String PriceUnit;
    public String PriceUnitOtherCurrency;
    public BigDecimal Value;
    public String ValueUnit;

    public BillingRunExcludedPriceComponentSummaryModel(BillingRunDocumentVatBaseProjection vatBaseProjection, String documentNumber, LocalDate documentDate) {
            this.DocumentDate =documentDate;
            this.DocumentNumber=documentNumber;
            this.TotalVolumes=vatBaseProjection.getTotalVolumes();
            this.PC=vatBaseProjection.getPriceComponent();
            this.TotalVolumesUnit=vatBaseProjection.getMeasureUnitForTotalVolumes();
            this.Price=vatBaseProjection.getPrice();
            this.PriceOtherCurrency=vatBaseProjection.getPriceInOtherCurrency();
            this.PriceUnit=vatBaseProjection.getMeasureUnitOfPrice();
            this.PriceUnitOtherCurrency=vatBaseProjection.getMeasureUnitOfPriceInOtherCurrency();
            this.Value=vatBaseProjection.getValue();
            this.ValueUnit=vatBaseProjection.getMeasureUnitOfValue();
    }
}
