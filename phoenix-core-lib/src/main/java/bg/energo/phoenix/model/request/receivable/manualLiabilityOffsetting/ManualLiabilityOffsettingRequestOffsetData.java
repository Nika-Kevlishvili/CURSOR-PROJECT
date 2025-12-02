package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ManualLiabilityOffsettingRequestOffsetData(
        @JsonProperty("d_id")
        Long dId,
        @JsonProperty("l_id")
        Long lId,
        @JsonProperty("r_id")
        Long rId,
        @JsonProperty("currency_id")
        @NotNull(message = "currencyId-Must not be null;")
        Long currencyId,
        @JsonProperty("offset_amount")
        @NotNull(message = "offsetAmount-Must not be null;")
        BigDecimal offsetAmount
) {
}
