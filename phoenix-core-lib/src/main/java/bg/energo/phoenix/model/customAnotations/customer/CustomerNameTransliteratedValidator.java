package bg.energo.phoenix.model.customAnotations.customer;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp= "^[\\dA-Z–@#$&* ()+\\-:.,'‘€№=\\s]*$",
        message="businessCustomerDetails.nameTranslated-Customer Name Translated Invalid Format or symbols;")
@Documented
@Constraint(validatedBy = {})
public @interface CustomerNameTransliteratedValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
