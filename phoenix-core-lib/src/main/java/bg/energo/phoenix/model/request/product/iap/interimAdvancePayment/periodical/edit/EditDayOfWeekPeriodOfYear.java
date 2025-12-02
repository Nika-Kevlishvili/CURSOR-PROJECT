package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit.EditDateRangesValidator;
import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit.EditDayOfWeekAndPeriodOfYearValidator;
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
@EditDayOfWeekAndPeriodOfYearValidator
public class EditDayOfWeekPeriodOfYear {

    @NotEmpty(message = "dayOfWeekAndPeriodOfYear.daysOfWeek-At least one value must be provided;")
    private Set<@Valid EditDayOfWeekRequest> daysOfWeek;

    @NotNull(message = "dayOfWeekAndPeriodOfYear.yearRound-Year-round is required;")
    private Boolean yearRound;

    @EditDateRangesValidator
    private List<@Valid EditPeriodOfYearRequest> periodsOfYear;

}
