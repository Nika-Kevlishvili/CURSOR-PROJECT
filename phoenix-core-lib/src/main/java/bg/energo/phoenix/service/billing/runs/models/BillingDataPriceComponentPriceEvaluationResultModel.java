package bg.energo.phoenix.service.billing.runs.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillingDataPriceComponentPriceEvaluationResultModel(
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        Boolean isShiftedHour,
        BigDecimal price
) {
}
