package bg.energo.phoenix.model.customAnotations.customer.manager;

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

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {AddressFieldValidator.AddressFieldValidatorImpl.class})
@Documented
public @interface AddressFieldValidator {

    String value() default "";

    String message() default "{value}-Invalid Format or symbols;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AddressFieldValidatorImpl implements ConstraintValidator<AddressFieldValidator, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (!StringUtils.isEmpty(value)) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[\\dA-ZА-Я–@#$&* ()+\\-:.,'‘€№=\\s]*$");
                Matcher matcher = pattern.matcher(value);
                return matcher.matches();
            }
            return true;
        }
    }
}
