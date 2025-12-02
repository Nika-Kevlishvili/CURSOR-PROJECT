package bg.energo.phoenix.model.customAnotations.customer;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Matcher;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidIBAN.IBANValidator.class})
public @interface ValidIBAN {

    String errorMessageKey() default "";

    String message() default "{errorMessageKey}-Invalid IBAN format;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IBANValidator implements ConstraintValidator<ValidIBAN, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (StringUtils.isNotEmpty(value)) {
                if (value.startsWith("BG")) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^BG\\d{2}[A-Z]{4}\\d{6}[A-Z\\d]{8}$");
                    Matcher matcher = pattern.matcher(value);
                    return matcher.matches();
                } else {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[A-Z]{2}[A-Z\\d]{12,32}$");
                    Matcher matcher = pattern.matcher(value);
                    return value.length() >= 14 && value.length() <= 34 && matcher.matches();
                }
            }

            return true;
        }

    }

}
