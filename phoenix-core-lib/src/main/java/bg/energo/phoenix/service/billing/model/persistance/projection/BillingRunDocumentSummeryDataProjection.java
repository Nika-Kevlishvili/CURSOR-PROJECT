package bg.energo.phoenix.service.billing.model.persistance.projection;

import java.math.BigDecimal;

public interface BillingRunDocumentSummeryDataProjection {
    String getDetailType();

    String getPriceComponent();

    BigDecimal getTotalVolumes();

    String getMeasureUnitForTotalVolumes();

    BigDecimal getPrice();

    BigDecimal getPriceOtherCurrency();

    String getMeasureUnitOfPrice();

    BigDecimal getValue();

    String getMeasureUnitOfValue();

    String getPriceComponentConnectionType();

    BigDecimal getVatRatePercent();

}
