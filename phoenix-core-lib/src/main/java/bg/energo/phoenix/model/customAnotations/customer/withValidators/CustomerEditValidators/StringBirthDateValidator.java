package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {StringBirthDateValidatorImpl.class})
public @interface StringBirthDateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
