package phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerEditValidators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {EditCustomerManagerValidatorImpl.class})
public @interface EditCustomerManagerValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
