package bg.energo.phoenix.model.response.billing.invoice;

import java.math.BigDecimal;

public interface InvoiceSummaryDataMiddleResponse {
    String getName();
    BigDecimal getTotalVolumes();
    String getMeasureUnitForTotalVolumes();
    BigDecimal getUnitPrice();
    String getMeasureUnitForUnitPrice();
    BigDecimal getValue();
    String getMeasureUnitForValue();
    String getIncomeAccountNumber();
    String getCostCenterControllingOrder();
    BigDecimal getVatRate();
    String getType();
}
