package bg.energo.phoenix.model.response.receivable.defaultInterestCalculation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DefaultInterestRateCalculationProcessResponse {
    private Long id;
    private BigDecimal lfp;
}
