package bg.energo.phoenix.model.process.latePaymentFIne;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ResultItemDTO {

    @JsonProperty("period_from")
    private LocalDate periodFrom;

    @JsonProperty("period_to")
    private LocalDate periodTo;

    private BigDecimal fee;

    @JsonProperty("currency_id")
    private Long currencyId;

    @JsonProperty("applicable_interest_rate")
    private BigDecimal applicableInterestRate;

    @JsonProperty("interval_amount")
    private BigDecimal intervalAmount;

    @JsonProperty("calculated_interest")
    private BigDecimal calculatedInterest;

}
