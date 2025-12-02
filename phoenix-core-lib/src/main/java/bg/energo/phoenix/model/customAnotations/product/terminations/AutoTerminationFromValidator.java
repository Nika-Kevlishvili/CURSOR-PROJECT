package bg.energo.phoenix.model.customAnotations.product.terminations;

import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;
import bg.energo.phoenix.model.request.product.termination.terminations.BaseTerminationRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {AutoTerminationFromValidator.AutoTerminationFromValidatorImpl.class})
public @interface AutoTerminationFromValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AutoTerminationFromValidatorImpl implements ConstraintValidator<AutoTerminationFromValidator, BaseTerminationRequest> {

        @Override
        public boolean isValid(BaseTerminationRequest request, ConstraintValidatorContext context) {
            if(request.getAutoTermination() != null){
                context.disableDefaultConstraintViolation();
                if(request.getAutoTermination() && request.getAutoTerminationFrom() == null){
                    context.buildConstraintViolationWithTemplate("autoTerminationFrom-Auto Termination From is required;")
                            .addConstraintViolation();
                    return false;
                }
                if(!request.getAutoTermination() && request.getAutoTerminationFrom() != null){
                    context.buildConstraintViolationWithTemplate("autoTerminationFrom-Auto Termination From must not be provided;")
                            .addConstraintViolation();
                    return false;
                }
            }else {
                context.disableDefaultConstraintViolation();
                if(request.getAutoTerminationFrom() != null){
                    context.buildConstraintViolationWithTemplate("autoTerminationFrom-Auto Termination From must not be provided;")
                            .addConstraintViolation();
                    return false;
                }
            }

            if(request.getAutoTermination().equals(true) && (request.getAutoTerminationFrom().equals(AutoTerminationFrom.EVENT_DATE) ||
                    request.getAutoTerminationFrom().equals(AutoTerminationFrom.FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE))){
                if(request.getEvent() == null){
                    context.buildConstraintViolationWithTemplate("event-[Event] shouldn't be null;")
                            .addConstraintViolation();
                    return false;
                }
            }
            //TODO TEMPLATE for delivery purpose - should be removed
            /*if(Boolean.TRUE.equals(request.getAutoEmailNotification())&& request.getTemplateId()==null){
                context.buildConstraintViolationWithTemplate("templateId-[templateId] shouldn't be null;")
                        .addConstraintViolation();
            }*/
            return true;
        }
    }

}
