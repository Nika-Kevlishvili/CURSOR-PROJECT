package bg.energo.phoenix.model.customAnotations.customer.unwantedCustomer;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UnwantedCustomerUICValidatorImpl.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UnwantedCustomerUICValidator {
    String message() default "identificationNumber-Invalid UIC symbols or length;";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
