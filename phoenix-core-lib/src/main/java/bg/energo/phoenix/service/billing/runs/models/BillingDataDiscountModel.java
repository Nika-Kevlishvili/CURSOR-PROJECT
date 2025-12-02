package bg.energo.phoenix.service.billing.runs.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingDataDiscountModel(
        Long modelId,
        Long pointOfDeliveryId,
        BigDecimal totalVolume,
        LocalDate periodFrom,
        LocalDate periodTo,
        Long priceComponentId,
        Long customerId
) {
}
