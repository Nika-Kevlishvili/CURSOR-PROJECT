package bg.energo.phoenix.service.billing.model.persistance.projection;

import java.math.BigDecimal;

public interface BillingRunDocumentVatBaseProjection {

    String getPodId();
    String getPriceComponent();
    BigDecimal getTotalVolumes();

    String getMeasureUnitForTotalVolumes();

    BigDecimal getPrice();

    BigDecimal getPriceInOtherCurrency();

    String getMeasureUnitOfPrice();

    String getMeasureUnitOfPriceInOtherCurrency();
    BigDecimal getValue();

    String getMeasureUnitOfValue();
}
