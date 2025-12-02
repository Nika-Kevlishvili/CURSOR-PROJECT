package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualLiabilityOffsettingCalculateOffsetResponse {
    @JsonProperty("d_id")
    private Long dId;
    @JsonProperty("l_id")
    private Long lId;
    @JsonProperty("r_id")
    private Long rId;
    @JsonProperty("currency_id")
    private Long currencyId;
    @JsonProperty("offset_amount")
    private BigDecimal offsetAmount;
}
