package phoenix.core.customer.model.customAnotations.customer.withValidators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/*
    Validate customer identifier based on customer Type:
    if LEGAL_ENTITY length must be 9 or 13
    if PRIVATE length must be 10 (Local customer) or 12 (Foreign customer)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CustomerIdentifierValidatorImpl.class})
public @interface CustomerIdentifierValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
