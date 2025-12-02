package phoenix.core.customer.model.customAnotations.customer.manager;

import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Matcher;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target( { FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PersonalNumber.PersonalNumberValidator.class})
public @interface PersonalNumber {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PersonalNumberValidator implements ConstraintValidator<PersonalNumber, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (!StringUtils.isEmpty(value)) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d{10}|\\d{12})$");
                Matcher matcher = pattern.matcher(value);
                return matcher.matches();
            }
            return true;
        }
    }
}
