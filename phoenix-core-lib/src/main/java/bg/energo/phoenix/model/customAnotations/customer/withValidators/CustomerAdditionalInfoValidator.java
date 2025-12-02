package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CustomerAdditionalInfoValidator.CustomerAdditionalInfoValidatorImpl.class})
public @interface CustomerAdditionalInfoValidator {

    String value() default "";

    String message() default "{value}-Invalid Format;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerAdditionalInfoValidatorImpl implements ConstraintValidator<CustomerAdditionalInfoValidator, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return !StringUtils.isBlank(value);
        }
    }
}
