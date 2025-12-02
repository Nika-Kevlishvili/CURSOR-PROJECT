package bg.energo.phoenix.model.response.billing.invoice;

import java.math.BigDecimal;

public record InvoiceReconnectionDto(
        BigDecimal taxValue,
        BigDecimal valueOfVat,
        BigDecimal totalAmountIncludingVat,
        BigDecimal vatRatePercent
) {
}
