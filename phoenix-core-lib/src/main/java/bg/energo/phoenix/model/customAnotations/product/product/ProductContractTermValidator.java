package bg.energo.phoenix.model.customAnotations.product.product;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target( {TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ProductContractTermValidatorImpl.class)
public @interface ProductContractTermValidator {
    String message() default "typeOfTerms-Invalid fields for selected type of terms;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
