package phoenix.core.customer.model.customAnotations.nomenclature;


import org.hibernate.validator.constraints.Length;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({ FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { })
@Length(min = 1,max = 32, message = "BIC does not match the allowed length")
@Pattern(regexp = "^[\\dA-ZА-Я-_]*$")
@Documented
public @interface BicDefaultValidator {

    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
