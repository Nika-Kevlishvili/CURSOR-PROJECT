package bg.energo.phoenix.model.customAnotations.nomenclature;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target( { FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp= "^[А-Яа-яA-Za-z\\d@&()+\\-.,№:–_\\s]*$", message="name-Pattern does not match the allowed symbols")
@Documented
@Constraint(validatedBy = {})
public @interface NameDefaultValidator {
    String message() default "name-Pattern does not match the allowed symbols";
    //represents group of constraints
    Class<?>[] groups() default {};
    //represents additional information about annotation
    Class<? extends Payload>[] payload() default {};
}
