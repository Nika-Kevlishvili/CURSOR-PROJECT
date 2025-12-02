package bg.energo.phoenix.service.billing.invoice.models;

import java.math.BigDecimal;

public record InvoiceDetailedDataAmountModel(
        BigDecimal vatRatePercent,
        BigDecimal pureAmount,
        Boolean isMainCurrency,
        BigDecimal alternativeCurrencyExchangeRate
) {
}
