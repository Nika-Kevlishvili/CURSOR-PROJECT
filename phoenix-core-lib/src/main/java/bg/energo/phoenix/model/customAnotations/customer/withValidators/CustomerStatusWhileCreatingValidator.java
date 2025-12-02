package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/*
    When customer is creating only two statuses must be allowed to assign: POTENTIAL and NEW
 */
@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CustomerStatusWhileCreatingValidatorImpl.class)
public @interface CustomerStatusWhileCreatingValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
