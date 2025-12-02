package phoenix.core.customer.model.customAnotations.customer;

import org.hibernate.validator.constraints.Length;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target( { FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp= "^[A-Za-z@#$&*()_+\\-§?!/\\\\<>:.,'€№=\\s]*$",
        message="Main Subject Activity Transl. Invalid Format or symbols")
@Documented
@Constraint(validatedBy = {})
@Length(min = 1, max = 2048)
public @interface MainSubjectActivityTranslatedValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
