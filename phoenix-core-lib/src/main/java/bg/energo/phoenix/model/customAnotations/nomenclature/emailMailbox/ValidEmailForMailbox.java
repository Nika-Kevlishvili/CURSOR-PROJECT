package bg.energo.phoenix.model.customAnotations.nomenclature.emailMailbox;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidEmailForMailbox.EmailForMailboxValidator.class})
@Documented
public @interface ValidEmailForMailbox {

    String field() default "";

    String message() default "Please provide a valid email address;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EmailForMailboxValidator implements ConstraintValidator<ValidEmailForMailbox, String> {

        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]+@[A-Za-z0-9.-]+\\.(com|net|org)$");
        private static final int MAX_LENGTH = 320;
        @Override
        public boolean isValid(String email, ConstraintValidatorContext context) {
            return email != null &&
                    EMAIL_PATTERN.matcher(email).matches() &&
                    email.length() <= MAX_LENGTH;
        }
    }
}
