package bg.energo.phoenix.model.customAnotations.product.product.vatRate;

import bg.energo.phoenix.model.request.nomenclature.product.VatRateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
/*
    when global rate is enabled request should have date field filled as well
    when global rate is disabled request should not have date field filled
 */
public class VatRateStartDateValidatorImpl implements ConstraintValidator<VatRateStartDateValidator, VatRateRequest> {
    @Override
    public boolean isValid(VatRateRequest vatRateRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean correct = true;
        String message = "";
        if (vatRateRequest.getGlobalVatRate() && vatRateRequest.getStartDate() == null){
            message = "startDate-startDate should not be empty when global rate is enabled";
            correct = false;
        }

        if (!vatRateRequest.getGlobalVatRate() && vatRateRequest.getStartDate() != null){
            message = "startDate-startDate should be empty when global rate is disabled";
            correct = false;
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return correct;
    }
}
