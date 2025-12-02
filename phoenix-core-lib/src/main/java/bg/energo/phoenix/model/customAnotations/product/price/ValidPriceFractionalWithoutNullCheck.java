package bg.energo.phoenix.model.customAnotations.product.price;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target( { METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ValidPriceFractionalWithoutNullCheck.List.class)
@Constraint(validatedBy = {PriceFractionPartValidatorNullable.class})
public @interface ValidPriceFractionalWithoutNullCheck {
    String value() default "";

    String fieldName() default "";

    String message() default "{value}-{fieldName} must contain max {fraction} digits after dot;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int fraction();

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {
        ValidPriceFractionalWithoutNullCheck[] value();
    }

}
