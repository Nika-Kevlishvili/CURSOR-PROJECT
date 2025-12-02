package bg.energo.phoenix.model.customAnotations.product.price;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PriceParameterTimeValidator.class)
@Target( { ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PriceTimeUpdateValidator {
    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
