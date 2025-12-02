package bg.energo.phoenix.model.customAnotations.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderListingRequest;
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
@Constraint(validatedBy = ValidReminderForDisconnectionListingRequest.ReminderForDisconnectionListingRequestValidator.class)
public @interface ValidReminderForDisconnectionListingRequest {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ReminderForDisconnectionListingRequestValidator implements ConstraintValidator<ValidReminderForDisconnectionListingRequest, PowerSupplyDisconnectionReminderListingRequest> {
        @Override
        public boolean isValid(PowerSupplyDisconnectionReminderListingRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateCreationDateRange(request, validationMessages);
            validateSendingDateRange(request, validationMessages);
            validateNumberOfCustomersRange(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessages.toString())
                        .addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }

        private void validateCreationDateRange(PowerSupplyDisconnectionReminderListingRequest request, StringBuilder validationMessages) {
            if (request.getCreationDateFrom() != null && request.getCreationDateTo() != null) {
                if (request.getCreationDateFrom().isAfter(request.getCreationDateTo())) {
                    validationMessages.append("creationDateFrom must be less than or equal to creationDateTo;");
                }
            }
        }

        private void validateSendingDateRange(PowerSupplyDisconnectionReminderListingRequest request, StringBuilder validationMessages) {
            if (request.getSendingDateFrom() != null && request.getSendingDateTo() != null) {
                if (request.getSendingDateFrom().isAfter(request.getSendingDateTo())) {
                    validationMessages.append("sendingDateFrom must be less than or equal to sendingDateTo;");
                }
            }
        }

        private void validateNumberOfCustomersRange(PowerSupplyDisconnectionReminderListingRequest request, StringBuilder validationMessages) {
            if (request.getNumberOfCustomersFrom() != null && request.getNumberOfCustomersTo() != null) {
                if (request.getNumberOfCustomersFrom().compareTo(request.getNumberOfCustomersTo()) > 0) {
                    validationMessages.append("numberOfCustomersFrom must be less than or equal to numberOfCustomersTo;");
                }
            }
        }
    }
}
