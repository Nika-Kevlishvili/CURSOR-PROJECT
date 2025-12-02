package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit.EditPaymentTermValidator;
import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit.EditValidationsForPeriodicalDateOfIssueType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EditPaymentTermValidator
@EditValidationsForPeriodicalDateOfIssueType
public class EditInterimAdvancePaymentRequest extends InterimAdvancePaymentBaseRequest{

    @Valid
    private EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest dayOfWeekAndPeriodOfYearAndDateOfMonth;

    @Valid
    private EditInterimAdvancePaymentTermRequest interimAdvancePaymentTerm;

}
