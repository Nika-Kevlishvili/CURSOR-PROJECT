package bg.energo.phoenix.model.customAnotations.contract;

import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.request.nomenclature.contract.TaskTypeStageRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.Range;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TaskTypeStagesValidator.TaskTypeStagesValidatorImpl.class})
public @interface TaskTypeStagesValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TaskTypeStagesValidatorImpl implements ConstraintValidator<TaskTypeStagesValidator, List<TaskTypeStageRequest>> {
        @Override
        public boolean isValid(List<TaskTypeStageRequest> taskTypeStageRequests, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder violations = new StringBuilder();

            if (CollectionUtils.isEmpty(taskTypeStageRequests)) {
                violations.append("taskTypeStages-Task Type Details must not be empty;");
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
                return false;
            }

            Set<Integer> stagesSet = new HashSet<>();

            Range<Long> termRange = Range.between(0L, 99L);
            for (int i = 0; i < taskTypeStageRequests.size(); i++) {
                TaskTypeStageRequest taskTypeStageRequest = taskTypeStageRequests.get(i);
                Long term = taskTypeStageRequest.getTerm();
                TermType termType = taskTypeStageRequest.getTermType();
                Integer stage = taskTypeStageRequest.getStage();
                if (term != null) {
                    if (!termRange.contains(term)) {
                        violations.append("taskTypeStages[%s].term-Term must be in range: [{%s}:{%s}];".formatted(i, termRange.getMinimum(), termRange.getMaximum()));
                    }
                }

                if (termType == null) {
                    violations.append("taskTypeStages[%s].termType-Term Type must not be null;".formatted(i));
                }

                if (stage == null) {
                    violations.append("taskTypeStages[%s].stage-Stage must not be null;".formatted(i));
                } else {
                    if (!stagesSet.add(stage)) {
                        violations.append("taskTypeStages[%s].stage-Duplicated stage found, stage must be unique;".formatted(i));
                    }
                }
                if(taskTypeStageRequest.getPerformerId()!=null && taskTypeStageRequest.getPerformerType()==null){
                    violations.append("taskTypeStages[%s].performerType-Performer type should be provided when performer is present;".formatted(i));
                }
            }

            if (violations.isEmpty()) {
                Optional<Integer> max = stagesSet.stream().max(Integer::compareTo);
                if (max.isEmpty()) {
                    violations.append("taskTypeStages-Task Type Stages maximal order not presented;");
                }

                Optional<Integer> min = stagesSet.stream().min(Integer::compareTo);
                if (min.isEmpty()) {
                    violations.append("taskTypeStages-Task Type Stages minimal order not presented;");
                }

                if (max.isPresent()) {
                    int maxStage = max.get();
                    Integer minStage = min.get();

                    if (!minStage.equals(1)) {
                        violations.append("taskTypeStages-Task Type Stages iteration must be started from 1;");
                    }

                    for (int i = minStage; i <= maxStage; i++) {
                        if (!stagesSet.contains(i)) {
                            violations.append("taskTypeStages-Task Type iteration must be incremented by 1, stage with order %s is missing;".formatted(i));
                        }
                    }
                }
            }

            if (!violations.isEmpty()) {
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }
    }
}
