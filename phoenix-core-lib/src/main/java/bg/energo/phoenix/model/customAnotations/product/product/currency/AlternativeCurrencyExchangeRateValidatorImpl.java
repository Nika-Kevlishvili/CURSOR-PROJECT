package bg.energo.phoenix.model.customAnotations.product.product.currency;

import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    when alternative currency is selected exchange rate should not be null
 */
public class AlternativeCurrencyExchangeRateValidatorImpl implements ConstraintValidator<AlternativeCurrencyExchangeRateValidator, CurrencyRequest> {
    private int maxLength = 12;
    @Override
    public boolean isValid(CurrencyRequest currencyRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean correct = true;
        String message = "";
        if (currencyRequest.getAltCurrencyId() != null && currencyRequest.getAltCurrencyExchangeRate() == null) {
            message = "altCurrencyExchangeRate-when alternative currency is selected exchange rate should not be null";
            correct = false;
        }

        String numberStringValue = "";
        if(currencyRequest.getAltCurrencyExchangeRate() != null)
            numberStringValue = currencyRequest.getAltCurrencyExchangeRate().toString().replace(".","");
        if(numberStringValue.length() > maxLength){
            message = "altCurrencyExchangeRate-number length exceeds maximum length limit, it should be in range 0.0000000001_9999999999.99";
            correct = false;
        }


        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return correct;
    }
}
