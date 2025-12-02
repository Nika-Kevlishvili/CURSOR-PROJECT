package bg.energo.phoenix.model.customAnotations.receivable.customerLiability;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class BigDecimalDigitsValidator implements ConstraintValidator<ValidBigDecimalDigits, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String valueStr = value.stripTrailingZeros().toPlainString();
        return valueStr.length() <= 32;
    }
}
