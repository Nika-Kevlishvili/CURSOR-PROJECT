package bg.energo.phoenix.model.customAnotations.product.goods;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@DecimalMax(value = "999999999999.99", message = "price-price must me less or equal to 999999999999.99;")
@DecimalMin(value = "0.01", message = "price-price must be greater or equal to 0.01;")
public @interface GoodsPriceValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
