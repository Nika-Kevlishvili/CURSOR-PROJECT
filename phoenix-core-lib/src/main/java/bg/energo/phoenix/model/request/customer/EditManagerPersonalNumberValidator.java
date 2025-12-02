package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;
import bg.energo.phoenix.util.epb.EPBFinalFields;
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

@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {EditManagerPersonalNumberValidator.ManagerPersonalNumberValidatorImpl.class})
public @interface EditManagerPersonalNumberValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ManagerPersonalNumberValidatorImpl implements ConstraintValidator<EditManagerPersonalNumberValidator, List<EditManagerRequest>>
    {

        @Override
        public boolean isValid(List<EditManagerRequest> requests, ConstraintValidatorContext context) {
            if(CollectionUtils.isEmpty(requests)){
                return true;
            }
            Set<String> personalNumbers=new HashSet<>();
            Set<Long> gdprIds=new HashSet<>();
            List<Integer> duplicatePersonalNumberIndexes=new ArrayList<>();
            int index=0;
            for (EditManagerRequest editManagerRequest : requests) {
                if(editManagerRequest.getPersonalNumber()==null){
                    continue;
                }
                String personalNumber = editManagerRequest.getPersonalNumber();
                if(personalNumber.equals(EPBFinalFields.GDPR)){
                    if(!gdprIds.add(editManagerRequest.getId())){
                        duplicatePersonalNumberIndexes.add(index);
                    }
                }else {
                    gdprIds.add(editManagerRequest.getId());
                    if(!personalNumbers.add(personalNumber)){
                        duplicatePersonalNumberIndexes.add(index);
                    }
                }

                index++;
            }

            if(!duplicatePersonalNumberIndexes.isEmpty()){
                StringBuilder sb = new StringBuilder();
                for (Integer duplicateIndex : duplicatePersonalNumberIndexes) {
                    sb.append(String.format("managers[%s].personalNumber - personal number is duplicated!",duplicateIndex));
                }
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
            return false;
            }
            return true;
        }
    }
}
