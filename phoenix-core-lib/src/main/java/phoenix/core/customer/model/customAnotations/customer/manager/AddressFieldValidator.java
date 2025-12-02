package phoenix.core.customer.model.customAnotations.customer.manager;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp= "^[\\dA-Za-zА-Яа-я–@#$&* ()+-:.,'€№=\\s]*$", message="Invalid Format or symbols")
@Documented
@Constraint(validatedBy = {})
public @interface AddressFieldValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
