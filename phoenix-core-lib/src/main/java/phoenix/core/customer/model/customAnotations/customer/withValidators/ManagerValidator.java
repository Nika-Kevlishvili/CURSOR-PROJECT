package phoenix.core.customer.model.customAnotations.customer.withValidators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/*
    When Customer Type is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
    minimum one manager is required
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ManagerValidatorImpl.class})
public @interface ManagerValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
