package bg.energo.phoenix.model.customAnotations.product.product;

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
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidEmail.EmailValidator.class})
@Documented
public @interface ValidEmail {

    String field() default "";

    String message() default "{field}-Please provide a valid email address in field {field};";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EmailValidator implements ConstraintValidator<ValidEmail, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (StringUtils.isEmpty(value)) {
                return true;
            }

            if (StringUtils.isBlank(value)) {
                return false;
            }

            Pattern pattern = Pattern.compile("^[A-Za-z\\d!@#$%&'*+\\-/=?^_`|{}~.]{1,256}$");
            Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }
    }

}
