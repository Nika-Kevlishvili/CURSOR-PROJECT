package bg.energo.phoenix.model.customAnotations.receivable.rescheduling;

import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingListingRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReschedulingListingRequest.ReschedulingListingRequestValidator.class)
public @interface ValidReschedulingListingRequest {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ReschedulingListingRequestValidator implements ConstraintValidator<ValidReschedulingListingRequest, ReschedulingListingRequest> {
        @Override
        public boolean isValid(ReschedulingListingRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateInstallmentRange(request, validationMessages);
            validateInstallmentDueDayRange(request, validationMessages);
            validateCreateDateRange(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessages.toString())
                        .addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }

        private void validateInstallmentRange(ReschedulingListingRequest request, StringBuilder validationMessages) {
            if (request.getNumberOfInstallmentFrom() != null && request.getNumberOfInstallmentTo() != null) {
                if (request.getNumberOfInstallmentFrom().compareTo(request.getNumberOfInstallmentTo()) > 0) {
                    validationMessages.append("numberOfInstallmentFrom must be less than or equal to numberOfInstallmentTo;");
                }
            }
        }

        private void validateInstallmentDueDayRange(ReschedulingListingRequest request, StringBuilder validationMessages) {
            if (request.getInstallmentDueDayFrom() != null && request.getInstallmentDueDayTo() != null) {
                if (request.getInstallmentDueDayFrom().compareTo(request.getInstallmentDueDayTo()) > 0) {
                    validationMessages.append("installmentDueDayFrom must be less than or equal to installmentDueDayTo;");
                }
            }
        }

        private void validateCreateDateRange(ReschedulingListingRequest request, StringBuilder validationMessages) {
            if (request.getCreateDateFrom() != null && request.getCreateDateTo() != null) {
                if (request.getCreateDateFrom().isAfter(request.getCreateDateTo())) {
                    validationMessages.append("createDateFrom must be less than or equal to createDateTo;");
                }
            }
        }
    }
}
