package bg.energo.phoenix.model.customAnotations.product.product.currency;

import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
/*
    when main currency  is enabled request should have date field filled as well
    when main currency is disabled request should not have date field filled
 */
public class MainCurrencyStartDateValidatorImpl implements ConstraintValidator<MainCurrencyStartDateValidator, CurrencyRequest> {
    @Override
    public boolean isValid(CurrencyRequest currencyRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean correct = true;
        String message = "";
        if (currencyRequest.getMainCurrency() && currencyRequest.getMainCurrencyStartDate() == null){
            message = "mainCurrencyStartDate-mainCurrencyStartDate should not be empty when main currency is enabled";
            correct = false;
        }

        if (!currencyRequest.getMainCurrency() && currencyRequest.getMainCurrencyStartDate() != null){
            message = "mainCurrencyStartDate-mainCurrencyStartDate should be empty when main currency is disabled";
            correct = false;
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return correct;
    }
}
