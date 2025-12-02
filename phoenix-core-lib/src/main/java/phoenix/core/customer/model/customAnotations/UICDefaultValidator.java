package phoenix.core.customer.model.customAnotations;

import phoenix.core.customer.util.UICValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UICValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UICDefaultValidator {
    String message() default "Invalid UIC";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
