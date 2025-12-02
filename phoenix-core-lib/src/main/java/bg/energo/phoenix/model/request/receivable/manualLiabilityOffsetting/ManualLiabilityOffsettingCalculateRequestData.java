package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ManualLiabilityOffsettingCalculateRequestData(
        @NotNull(message = "id-Must not be null;")
        Long id,

        @JsonProperty("currency_id")
        @NotNull(message = "currencyId-Must not be null;")
        Long currencyId,

        @JsonProperty("current_amount")
        @NotNull(message = "currentAmount-Must not be null;")
        BigDecimal currentAmount
) {
}
