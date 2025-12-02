package bg.energo.phoenix.model.customAnotations.product.product.vatRate;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.VatRateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
/*
    when global rate is enabled status should be active only
 */
public class GlobalVatRateAndStatusValidatorImpl implements ConstraintValidator<GlobalVatRateAndStatusValidator, VatRateRequest> {
    @Override
    public boolean isValid(VatRateRequest vatRateRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean correct = true;
        String message = "";
        if (vatRateRequest.getGlobalVatRate() && vatRateRequest.getStatus() != NomenclatureItemStatus.ACTIVE){
            message = "status-when global vat rate is enabled status should be ACTIVE";
            correct = false;
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return correct;
    }
}
