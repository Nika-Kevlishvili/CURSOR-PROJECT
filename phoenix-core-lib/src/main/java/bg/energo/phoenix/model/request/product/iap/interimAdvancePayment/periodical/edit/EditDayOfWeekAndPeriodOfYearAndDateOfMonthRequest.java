package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit.EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidator;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidator
public class EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest {

    @NotNull(message = "periodType-Period Type is required;")
    private PeriodType periodType;

    @Valid
    private EditDayOfWeekPeriodOfYear dayOfWeekAndPeriodOfYear;

    private Set<@Valid EditDateOfMonthRequest> dateOfMonths;

}
