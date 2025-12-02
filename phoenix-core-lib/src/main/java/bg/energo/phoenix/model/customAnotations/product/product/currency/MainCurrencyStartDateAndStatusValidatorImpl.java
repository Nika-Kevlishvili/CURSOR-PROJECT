package bg.energo.phoenix.model.customAnotations.product.product.currency;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    when main currency is enabled status should be active only
 */
public class MainCurrencyStartDateAndStatusValidatorImpl implements ConstraintValidator<MainCurrencyStartDateAndStatusValidator, CurrencyRequest> {
    @Override
    public boolean isValid(CurrencyRequest currencyRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean correct = true;
        String message = "";
        if (currencyRequest.getMainCurrency() && currencyRequest.getStatus() != NomenclatureItemStatus.ACTIVE){
            message = "status-when main currency is enabled status should be ACTIVE";
            correct = false;
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return correct;
    }
}
