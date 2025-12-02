package bg.energo.phoenix.model.customAnotations;

import bg.energo.phoenix.util.epb.EPBListUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DuplicatedValuesValidator.DuplicatedValuesValidatorImpl.class})
public @interface DuplicatedValuesValidator {
    String fieldPath() default "";

    String message() default "Duplicated values found in list;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DuplicatedValuesValidatorImpl implements ConstraintValidator<DuplicatedValuesValidator, List<?>> {
        private String fieldPath = "";

        @Override
        public void initialize(DuplicatedValuesValidator constraintAnnotation) {
            this.fieldPath = constraintAnnotation.fieldPath();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(List<?> list, ConstraintValidatorContext context) {
            if (CollectionUtils.isNotEmpty(list)) {
                StringBuilder exceptionMessagesBuilder = new StringBuilder();

                exceptionMessagesBuilder.append(EPBListUtils.validateDuplicateValuesByIndexes(list, this.fieldPath));

                if (!exceptionMessagesBuilder.isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(exceptionMessagesBuilder.toString()).addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }
}
