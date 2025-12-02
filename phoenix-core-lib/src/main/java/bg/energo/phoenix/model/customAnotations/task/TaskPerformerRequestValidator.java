package bg.energo.phoenix.model.customAnotations.task;

import bg.energo.phoenix.model.request.task.TaskPerformerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TaskPerformerRequestValidator.TaskPerformerRequestValidatorImpl.class})
public @interface TaskPerformerRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TaskPerformerRequestValidatorImpl implements ConstraintValidator<TaskPerformerRequestValidator, List<TaskPerformerRequest>> {
        @Override
        public boolean isValid(List<TaskPerformerRequest> requests, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            for (TaskPerformerRequest request : requests) {
                if (request.getPerformerType() != null && request.getPerformer() == null) {
                    stringBuilder.append("taskPerformerRequests[%s].performer-Performer cannot be null;".formatted(i));
                } else if (request.getPerformer() != null && request.getPerformerType() == null) {
                    stringBuilder.append("taskPerformerRequests[%s].performerType-Performer Type cannot be null;".formatted(i));
                }
                i++;
            }

            if (!stringBuilder.isEmpty()) {
                context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }

}
