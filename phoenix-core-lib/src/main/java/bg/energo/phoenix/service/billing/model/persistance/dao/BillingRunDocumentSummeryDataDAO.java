package bg.energo.phoenix.service.billing.model.persistance.dao;

import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentSummeryDataProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class BillingRunDocumentSummeryDataDAO {
    private String detailType;
    private String priceComponent;
    private BigDecimal totalVolumes;
    private String measureUnitForTotalVolumes;
    private BigDecimal price;
    private BigDecimal priceOtherCurrency;
    private String measureUnitOfPrice;
    private BigDecimal value;
    private String measureUnitOfValue;
    private BigDecimal vatRatePercent;
    private PriceComponentConnectionType priceComponentConnectionType;
    private String priceComponentGroup;


    public BillingRunDocumentSummeryDataDAO(String priceComponent,
                                            BigDecimal totalVolumes,
                                            String measureUnitForTotalVolumes,
                                            BigDecimal price,
                                            BigDecimal priceOtherCurrency,
                                            BigDecimal value,
                                            String measureUnitOfValue,
                                            BigDecimal vatRatePercent,
                                            String priceComponentGroup) {
        this.priceComponent = priceComponent;
        this.totalVolumes = totalVolumes;
        this.measureUnitForTotalVolumes = measureUnitForTotalVolumes;
        this.price = price;
        this.priceOtherCurrency = priceOtherCurrency;
        this.value = value;
        this.measureUnitOfValue = measureUnitOfValue;
        this.vatRatePercent = vatRatePercent;
        this.priceComponentGroup = priceComponentGroup;
    }

    public BillingRunDocumentSummeryDataDAO(BillingRunDocumentSummeryDataProjection projection) {
        this.detailType = projection.getDetailType();
        this.priceComponent = projection.getPriceComponent();
        this.totalVolumes = projection.getTotalVolumes();
        this.measureUnitForTotalVolumes = projection.getMeasureUnitForTotalVolumes();
        this.price = projection.getPrice();
        this.priceOtherCurrency = projection.getPriceOtherCurrency();
        this.measureUnitOfPrice = projection.getMeasureUnitOfPrice();
        this.vatRatePercent = projection.getVatRatePercent();
        this.value = projection.getValue();
        this.measureUnitOfValue = projection.getMeasureUnitOfValue();
        this.priceComponentConnectionType = PriceComponentConnectionType.valueOf(projection.getPriceComponentConnectionType());

    }

    public enum PriceComponentConnectionType {
        DIRECT,
        GROUP,
        FROM_PC_GROUP
    }
}
