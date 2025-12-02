package bg.energo.phoenix.model.response.billing.invoice;

import java.math.BigDecimal;

public record InvoiceConnectionsShortResponse(
        Long id,
        BigDecimal initialAmount,
        BigDecimal currentAmount
) {
}
