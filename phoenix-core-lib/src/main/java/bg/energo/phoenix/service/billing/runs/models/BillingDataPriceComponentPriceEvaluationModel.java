package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record BillingDataPriceComponentPriceEvaluationModel(
        Long priceComponentId,
        Long pointOfDeliveryId,
        Long productContractDetailId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
        PeriodType dimension,
        TimeZone timeZone
) {
}
