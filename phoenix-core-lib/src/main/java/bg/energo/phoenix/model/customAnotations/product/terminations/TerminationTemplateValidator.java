package bg.energo.phoenix.model.customAnotations.product.terminations;

import bg.energo.phoenix.model.request.product.termination.terminations.BaseTerminationRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TerminationTemplateValidator.TerminationTemplateValidatorImpl.class})
public @interface TerminationTemplateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TerminationTemplateValidatorImpl implements ConstraintValidator<TerminationTemplateValidator, BaseTerminationRequest> {

        @Override
        public boolean isValid(BaseTerminationRequest request, ConstraintValidatorContext context) {
            //TODO TEMPLATE for delivery purpose - should be removed
           /* Set<TerminationNotificationChannelType> terminationNotificationChannels = request.getTerminationNotificationChannels();
            if (CollectionUtils.isEmpty(terminationNotificationChannels)) {
                return true;
            }
            if (terminationNotificationChannels.contains(TerminationNotificationChannelType.EMAIL) && request.getTemplateId() == null) {
                context.buildConstraintViolationWithTemplate("templateId-[templateId] template id can not be null!;").addConstraintViolation();
                return false;//TODO TEMPLATE for delivery purpose - should be removed
            }*/
            return true;
        }


    }
}
