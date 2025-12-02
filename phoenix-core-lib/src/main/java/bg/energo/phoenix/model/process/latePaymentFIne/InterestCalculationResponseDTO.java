package bg.energo.phoenix.model.process.latePaymentFIne;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class InterestCalculationResponseDTO {
    @JsonProperty("calculated_interest")
    private BigDecimal calculatedInterest;

    private List<ResultItemDTO> results;
}
