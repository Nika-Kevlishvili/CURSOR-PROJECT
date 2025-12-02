package bg.energo.phoenix.model.customAnotations.receivable.customerLiability;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BigDecimalDigitsValidator.class)
public @interface ValidBigDecimalDigits {
    String message() default "Invalid number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

