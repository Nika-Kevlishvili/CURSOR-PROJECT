package bg.energo.phoenix.model.customAnotations.task;

import bg.energo.phoenix.model.request.task.TaskPerformerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.Range;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.*;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TaskPerformersValidator.TaskPerformersValidatorImpl.class})
public @interface TaskPerformersValidator {
    boolean isUpdate() default false;

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TaskPerformersValidatorImpl implements ConstraintValidator<TaskPerformersValidator, List<TaskPerformerRequest>> {
        private boolean isUpdate;

        @Override
        public void initialize(TaskPerformersValidator constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.isUpdate = constraintAnnotation.isUpdate();
        }

        @Override
        public boolean isValid(List<TaskPerformerRequest> taskPerformerRequests, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder violations = new StringBuilder();

            Set<Integer> taskStagesSet = new HashSet<>();

            Range<Long> termRange = Range.between(0L, 99L);
            for (int i = 0; i < taskPerformerRequests.size(); i++) {
                TaskPerformerRequest taskPerformerRequest = taskPerformerRequests.get(i);

                LocalDate startDate = taskPerformerRequest.getStartDate();

                Long term = taskPerformerRequest.getTerm();

                if (Objects.nonNull(startDate) && startDate.isBefore(LocalDate.now())) {
                    violations.append("taskPerformerRequests[%s].startDate-Start date cannot be updated to past date;".formatted(i));
                    isValid = false;
                }

                if (!isUpdate) {
                    if (i == 0) {
                        // first stage validation
                        if (startDate == null) {
                            violations.append("taskPerformerRequests[%s].startDate-First performer start and end date must not be null;".formatted(i));
                            isValid = false;
                        }

                        if (term == null) {
                            violations.append("taskPerformerRequests[%s].term-Term must be in range: [{%s}:{%s}];".formatted(i, termRange.getMinimum(), termRange.getMaximum()));
                            isValid = false;
                        }
                    } else {
                        if (startDate != null) {
                            violations.append("taskPerformerRequests[%s].startDate-Only first performer start and end date should be filled;".formatted(i));
                            isValid = false;
                        }
                    }
                }

                if (taskPerformerRequest.getStage() == null) {
                    violations.append("taskPerformerRequests[%s].stage-Stage must not be null;".formatted(i));
                    isValid = false;
                } else {
                    if (!taskStagesSet.add(taskPerformerRequest.getStage())) {
                        violations.append("taskPerformerRequests[%s].stage-Duplicated stage found, stage must be unique;".formatted(i));
                        isValid = false;
                    }
                }

                if (term != null) {
                    if (!termRange.contains(term)) {
                        violations.append("taskPerformerRequests[%s].term-Term must be in range: [{%s}:{%s}];".formatted(i, termRange.getMinimum(), termRange.getMaximum()));
                        isValid = false;
                    }
                }

                if (taskPerformerRequest.getStage() == null) {
                    violations.append("taskPerformerRequests[%s].stage-Stage must not be null;".formatted(i));
                    isValid = false;
                }
            }

            if (isValid) {
                // stage order validation
                Optional<Integer> max = taskStagesSet.stream().max(Integer::compareTo);
                if (max.isEmpty()) {
                    violations.append("taskPerformerRequests-Task Type Stages maximal order not presented;");
                    isValid = false;
                }

                Optional<Integer> min = taskStagesSet.stream().min(Integer::compareTo);
                if (min.isEmpty()) {
                    violations.append("taskPerformerRequests-Task Type Stages minimal order not presented;");
                }

                if (max.isPresent()) {
                    int maxStage = max.get();
                    Integer minStage = min.get();

                    if (!minStage.equals(1)) {
                        violations.append("taskPerformerRequests-Task Stages iteration must be started from 1;");
                        isValid = false;
                    }

                    for (int i = minStage; i <= maxStage; i++) {
                        if (!taskStagesSet.contains(i)) {
                            violations.append("taskPerformerRequests-Task Stages iteration must be incremented by 1, stage with order %s is missing;".formatted(i));
                            isValid = false;
                        }
                    }
                }
            }

            if (!isValid) {
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
