package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.DateRangesValidator;
import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.DayOfWeekAndPeriodOfYearValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@DayOfWeekAndPeriodOfYearValidator
public class DayOfWeekAndPeriodOfYear {

    @NotEmpty(message = "dayOfWeekAndPeriodOfYear.daysOfWeek-At least one value must be provided;")
    private Set<@Valid DayOfWeekBaseRequest> daysOfWeek;

    @NotNull(message = "dayOfWeekAndPeriodOfYear.yearRound-Year-round is required;")
    private Boolean yearRound;

    @DateRangesValidator
    private List<@Valid PeriodOfYearBaseRequest> periodsOfYear;

}
