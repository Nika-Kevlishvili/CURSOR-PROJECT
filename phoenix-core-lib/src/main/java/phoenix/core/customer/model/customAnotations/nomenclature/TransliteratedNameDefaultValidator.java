package phoenix.core.customer.model.customAnotations.nomenclature;

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
@Pattern(regexp= "^[A-Za-z\\d@&()+-.,№\\s]*$", message="Pattern does not match the allowed symbols")
@Documented
@Constraint(validatedBy = {})
public @interface TransliteratedNameDefaultValidator {
    String message() default "Pattern does not match the allowed symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
