package bg.energo.phoenix.model.customAnotations.product.price;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class PriceFractionPartValidatorNullable implements ConstraintValidator<ValidPriceFractionalWithoutNullCheck, BigDecimal> {
    private int maxFractionLength;

    @Override
    public void initialize(ValidPriceFractionalWithoutNullCheck constraintAnnotation) {
        this.maxFractionLength = constraintAnnotation.fraction();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if(value==null){
            return true;
        }
        return maxFractionLength >= (Math.max(value.scale(), 0));
    }
}
