package bg.energo.phoenix.model.customAnotations.customer.manager;

import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ManagerPersonalNumberValidator.ManagerPersonalNumberValidatorImpl.class})
public @interface ManagerPersonalNumberValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ManagerPersonalNumberValidatorImpl implements ConstraintValidator<ManagerPersonalNumberValidator, List<CreateManagerRequest>> {

        @Override
        public boolean isValid(List<CreateManagerRequest> value, ConstraintValidatorContext context) {
            if(CollectionUtils.isEmpty(value)){
                return true;
            }
            Set<String> personalNumbers = new HashSet<>();
            List<Integer> duplicatePersonalNumberIndexes = new ArrayList<>();
            int index = 0;
            for (CreateManagerRequest createManagerRequest : value) {
                if (createManagerRequest.getPersonalNumber() == null) {
                    continue;
                }
                String personalNumber = createManagerRequest.getPersonalNumber();
                if (!personalNumbers.add(personalNumber)) {
                    duplicatePersonalNumberIndexes.add(index);
                }
                index++;
            }

            if (!duplicatePersonalNumberIndexes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Integer duplicateIndex : duplicatePersonalNumberIndexes) {
                    sb.append(String.format("managers[%s].personalNumber - personal number is duplicated!", duplicateIndex));
                }
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}
