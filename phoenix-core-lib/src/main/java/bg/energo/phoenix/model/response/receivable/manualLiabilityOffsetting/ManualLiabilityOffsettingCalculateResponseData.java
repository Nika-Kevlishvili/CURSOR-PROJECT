package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualLiabilityOffsettingCalculateResponseData {
    private Long id;
    @JsonProperty("currency_id")
    private Long currencyId;
    @JsonProperty("current_amount")
    private BigDecimal currentAmount;
}
