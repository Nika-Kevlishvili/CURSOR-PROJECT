package bg.energo.phoenix.model.customAnotations.product.price;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class FractionalPartValidator implements ConstraintValidator<ValidFractionalPart, BigDecimal> {

    private int maxFractionLength;
    private boolean nullable;

    @Override
    public void initialize(ValidFractionalPart constraintAnnotation) {
        this.maxFractionLength = constraintAnnotation.fraction();
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (nullable) {
            return true;
        } else {
            if (value == null) {
                context.buildConstraintViolationWithTemplate("").addConstraintViolation();
                return false;
            }

            return maxFractionLength >= (Math.max(value.scale(), 0));
        }
    }

}
