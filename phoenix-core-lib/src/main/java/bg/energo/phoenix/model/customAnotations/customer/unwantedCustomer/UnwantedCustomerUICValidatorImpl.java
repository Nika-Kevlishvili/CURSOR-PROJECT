package bg.energo.phoenix.model.customAnotations.customer.unwantedCustomer;

import bg.energo.phoenix.util.customer.IdentificationNumberChecker;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnwantedCustomerUICValidatorImpl implements ConstraintValidator<UnwantedCustomerUICValidator, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean isValid = false;
        Pattern pattern = Pattern.compile("^[0-9A-Z/–-]+$");
        if(StringUtils.isNotEmpty(value)) {
            value = value.trim();
            Matcher matcher = pattern.matcher(value);
            isValid = matcher.matches();
            if (value.length() == 10 && isValid) {
                isValid = IdentificationNumberChecker.checkIdentificationNumber(value);
            }
        }
        return isValid;
    }
}
