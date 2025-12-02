package bg.energo.phoenix.model.customAnotations.customer;

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
@Pattern(regexp= "^\\d{0,7}(\\.\\d{1,3})?$",
        message="bankingDetails.declaredConsumption-Customer Declared Consumption Invalid Format or symbols;")
@Documented
@Constraint(validatedBy = {})
public @interface CustomerDeclaredConsumptionValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
