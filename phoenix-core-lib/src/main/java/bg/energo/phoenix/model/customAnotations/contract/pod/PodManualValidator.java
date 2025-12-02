package bg.energo.phoenix.model.customAnotations.contract.pod;

import bg.energo.phoenix.model.request.contract.pod.PodManualActivationRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PodManualValidator.PodManualValidatorImpl.class})
public @interface PodManualValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodManualValidatorImpl implements ConstraintValidator<PodManualValidator, PodManualActivationRequest> {

        @Override
        public boolean isValid(PodManualActivationRequest value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            if(value.getActivationDate()==null&& value.getDeactivationDate()!=null){
                context.buildConstraintViolationWithTemplate("activationDate-Activation date can not be null if deactivation is provided!;").addConstraintViolation();
                return false;
            }

            if(value.getActivationDate()!=null&&value.getDeactivationDate()!=null && value.getActivationDate().isAfter(value.getDeactivationDate())){
                context.buildConstraintViolationWithTemplate("deactivationDate-Deactivation Date can not be less than activation date!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
