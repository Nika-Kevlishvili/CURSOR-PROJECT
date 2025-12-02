package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.util.epb.EPBFinalFields;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class StringBirthDateValidatorImpl implements ConstraintValidator<StringBirthDateValidator, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(StringUtils.equals(EPBFinalFields.GDPR, value)||value==null){
            return true;
        }
        String regex = "^\\d{4}[\\-\\/\\s]?((((0[13578])|(1[02]))[\\-\\/\\s]?(([0-2][0-9])|(3[01])))|(((0[469])|(11))[\\-\\/\\s]?(([0-2][0-9])|(30)))|(02[\\-\\/\\s]?[0-2][0-9]))$";
        return Pattern.matches(regex, value);
    }
}
