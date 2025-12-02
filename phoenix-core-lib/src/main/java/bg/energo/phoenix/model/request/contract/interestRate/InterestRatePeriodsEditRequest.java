package bg.energo.phoenix.model.request.contract.interestRate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestRatePeriodsEditRequest {

    private Long id;

    @NotNull(message = "amountInPercent-[AmountInPercent] Must not be null;")
    @DecimalMin(value = "0.00", message = "amountInPercent-[amountInPercent] should be more than 0.00;")
    @DecimalMax(value = "9999.99", message = "amountInPercent-[amountInPercent] should be less than 9999.99;")
    private BigDecimal amountInPercent;

    @DecimalMin(value = "0.00", message = "baseInterestRate-[baseInterestRate] should be more than 0.00;")
    @DecimalMax(value = "9999.99", message = "baseInterestRate-[baseInterestRate] should be less than 9999.99;")
    private BigDecimal baseInterestRate;

    @DecimalMin(value = "0.00", message = "fee-[fee] should be more than 0.00;")
    @DecimalMax(value = "9999.99", message = "fee-[fee] should be less than 9999.99;")
    private BigDecimal fee;

    @NotNull(message = "currencyId-[CurrencyId] Must not be null;")
    private Long currencyId;

    @NotNull(message = "validFrom-[ValidFrom] Must not be null;")
    private LocalDate validFrom;

}
