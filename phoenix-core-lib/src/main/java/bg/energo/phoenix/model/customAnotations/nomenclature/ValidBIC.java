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

@Target({ FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { })
@Pattern(regexp = "^[\\dA-Z-–]{1,32}$", message = "bic-BIC does not match allowed pattern or length;")
@Documented
public @interface ValidBIC {

    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}