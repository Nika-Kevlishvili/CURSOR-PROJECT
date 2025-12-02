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
@Constraint(validatedBy = {BusinessActivityNameTranslatedValidator.BusinessActivityNameTranslatedValidatorImpl.class})
public @interface BusinessActivityNameTranslatedValidator {
    String value() default "";

    String message() default "{value}-Invalid Format or symbols;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BusinessActivityNameTranslatedValidatorImpl implements ConstraintValidator<BusinessActivityNameTranslatedValidator, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            } else if (StringUtils.isBlank(value)) {
                return false;
            } else {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[A-Za-z0-9–@#$&*()+\\-:.,‘€№= ]*$");
                Matcher matcher = pattern.matcher(value);
                return matcher.matches();
            }
        }
    }
}
