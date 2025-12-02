package bg.energo.phoenix.model.customAnotations.product.applicationModel;

import bg.energo.phoenix.util.RRuleUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;
import org.dmfs.rfc5545.recur.RecurrenceRule;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {RRuleValidator.RRuleValidatorImpl.class})
public @interface RRuleValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class RRuleValidatorImpl implements ConstraintValidator<RRuleValidator, String> {

        @Override
        public boolean isValid(String rruleString, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            if (StringUtils.isEmpty(rruleString)) {
                return true;
            }
            RecurrenceRule recurrenceRule = RRuleUtil.validRecurrenceRule(rruleString);
            if (recurrenceRule == null) {
                context.buildConstraintViolationWithTemplate("RRULE is invalid;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
