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
@Constraint(validatedBy = {NoticeDueFieldsValidator.NoticeDueFieldsValidatorImpl.class})
public @interface NoticeDueFieldsValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NoticeDueFieldsValidatorImpl implements ConstraintValidator<NoticeDueFieldsValidator, BaseTerminationRequest> {

        @Override
        public boolean isValid(BaseTerminationRequest request, ConstraintValidatorContext context) {
            boolean result;
            if (request.getNoticeDue() == null || !request.getNoticeDue()) {
                result = fieldsAreNull(request, context);
            } else {
                result = fieldsAreNotNull(request, context);
            }
            return result;
        }

        private boolean fieldsAreNotNull(BaseTerminationRequest request, ConstraintValidatorContext context) {
            boolean result = true;
            if (request.getNoticeDueValueMin() == null) {
                context.buildConstraintViolationWithTemplate("noticePeriodValue-Notice Period Value Min is required;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getNoticeDueValueMax() != null) {
                if (request.getNoticeDueValueMin() > request.getNoticeDueValueMax()) {
                    context.buildConstraintViolationWithTemplate("noticePeriodValue-Notice Period Value Min should be less or equal to Value Max if Value Max is filled;")
                            .addConstraintViolation();
                    result = false;
                }
            }
            if (request.getNoticeDueType() == null) {
                context.buildConstraintViolationWithTemplate("noticePeriodType-Notice Period Type is required;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getAutoEmailNotification() == null) {
                context.buildConstraintViolationWithTemplate("autoEmailNotification-Auto Email Notification is required;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getCalculateFrom() == null) {
                context.buildConstraintViolationWithTemplate("calculateFrom-Calculate From is required;")
                        .addConstraintViolation();
                result = false;
            }
            return result;
        }

        private boolean fieldsAreNull(BaseTerminationRequest request, ConstraintValidatorContext context) {
            boolean result = true;
            if (request.getNoticeDueValueMin() != null) {
                context.buildConstraintViolationWithTemplate("noticePeriodValueMin-Notice Period Value Min must not be provided;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getNoticeDueValueMax() != null) {
                context.buildConstraintViolationWithTemplate("noticePeriodValue-Notice Period Value Max must not be provided;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getNoticeDueType() != null) {
                context.buildConstraintViolationWithTemplate("noticePeriodType-Notice Period Type must not be provided;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getAutoEmailNotification() != null) {
                context.buildConstraintViolationWithTemplate("autoEmailNotification-Auto Email Notification must not be provided;")
                        .addConstraintViolation();
                result = false;
            }
            if (request.getCalculateFrom() != null) {
                context.buildConstraintViolationWithTemplate("calculateFrom-Calculate From must not be provided;")
                        .addConstraintViolation();
                result = false;
            }
            return result;
        }
    }

}
