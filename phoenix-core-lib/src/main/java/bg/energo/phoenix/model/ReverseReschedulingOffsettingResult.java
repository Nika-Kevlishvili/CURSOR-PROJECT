package bg.energo.phoenix.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ReverseReschedulingOffsettingResult {
    private BigDecimal offsettingAmount;
    private Integer offsettingCurrencyId;
    private String message;
}
