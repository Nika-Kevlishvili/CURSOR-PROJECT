package phoenix.core.customer.model.customAnotations.customer;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

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
@Length(min = 1, max = 512)
@Documented
@Constraint(validatedBy = {PrivateCustomerNameTranslatedValidator.PrivateCustomerNameTranslatedValidatorImpl.class})
public @interface PrivateCustomerNameTranslatedValidator {

    String value() default "";
    String message() default "{value}: Invalid Format or symbols; ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PrivateCustomerNameTranslatedValidatorImpl implements ConstraintValidator<PrivateCustomerNameTranslatedValidator, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (!StringUtils.isEmpty(value)) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[\\dA-Za-z–& \\-'\\s]*$");
                Matcher matcher = pattern.matcher(value);
                return matcher.matches();
            }
            return true;
        }
    }
}
