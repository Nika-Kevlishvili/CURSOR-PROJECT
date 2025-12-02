package bg.energo.phoenix.model.response.contract.InterestRate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRatePeriodsResponse {

    private Long id;

    private BigDecimal amountInPercent;

    private BigDecimal baseInterestRate;

    private BigDecimal applicableInterestRate;

    private BigDecimal fee;

    private Long currencyId;

    private String currencyName;

    private LocalDate validFrom;

}
