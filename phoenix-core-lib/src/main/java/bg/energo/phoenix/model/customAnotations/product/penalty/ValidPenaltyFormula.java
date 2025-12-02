package bg.energo.phoenix.model.customAnotations.product.penalty;

import bg.energo.phoenix.service.contract.action.calculation.formula.PenaltyFormulaEvaluator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidPenaltyFormula.PenaltyFormulaValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPenaltyFormula {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PenaltyFormulaValidator implements ConstraintValidator<ValidPenaltyFormula, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (StringUtils.isEmpty(value)) {
                return true;
            }

            return PenaltyFormulaEvaluator.isValidPenaltyFormula(value);
        }

    }

}
