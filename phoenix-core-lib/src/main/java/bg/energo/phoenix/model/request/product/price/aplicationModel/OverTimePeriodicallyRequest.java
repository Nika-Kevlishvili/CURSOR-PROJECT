package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.PeriodicityValidator;
import bg.energo.phoenix.model.customAnotations.product.applicationModel.RRuleValidator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDateOfMonthRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekPeriodOfYear;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
@PeriodicityValidator
public class OverTimePeriodicallyRequest {
    @NotNull(message = "applicationModelRequest.overTimePeriodicallyRequest.periodType-Period Type is required;")
    private Periodicity periodType;

    @Valid
    private EditDayOfWeekPeriodOfYear dayOfWeekAndPeriodOfYear;

    private Set<@Valid EditDateOfMonthRequest> dateOfMonths;
    @RRuleValidator(message = "applicationModelRequest.overTimePeriodicallyRequest.formula")
    private String formula;
}
