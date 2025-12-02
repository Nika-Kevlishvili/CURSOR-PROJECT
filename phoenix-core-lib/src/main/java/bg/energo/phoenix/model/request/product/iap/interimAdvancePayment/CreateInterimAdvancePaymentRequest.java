package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.CreatePaymentTermValidator;
import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.ValidationsForPeriodicalDateOfIssueType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DayOfWeekAndPeriodOfYearAndDateOfMonthRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@CreatePaymentTermValidator
@ValidationsForPeriodicalDateOfIssueType
public class CreateInterimAdvancePaymentRequest extends InterimAdvancePaymentBaseRequest {

    @Valid
    private DayOfWeekAndPeriodOfYearAndDateOfMonthRequest dayOfWeekAndPeriodOfYearAndDateOfMonth;

    @Valid
    private CreateInterimAdvancePaymentTermRequest interimAdvancePaymentTerm;

}
