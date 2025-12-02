package bg.energo.phoenix.model.customAnotations.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = ReminderForDisconnectionCreateRequestValidator.ReminderForDisconnectionCreateRequestValidatorImpl.class)
public @interface ReminderForDisconnectionCreateRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ReminderForDisconnectionCreateRequestValidatorImpl implements ConstraintValidator<ReminderForDisconnectionCreateRequestValidator, PowerSupplyDisconnectionReminderBaseRequest> {
        @Override
        public boolean isValid(PowerSupplyDisconnectionReminderBaseRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateSendToCustomerDateTime(request, validationMessages);
            validateLiabilityAmounts(request, validationMessages);
            validateCurrency(request, validationMessages);
            validateSendToCustomerDateTimeAndLiabilitiesMaxDueDate(request, validationMessages);
            validateTemplates(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateSendToCustomerDateTime(PowerSupplyDisconnectionReminderBaseRequest request, StringBuilder validationMessages) {
            LocalDateTime sendToCustomerDateTime = request.getCustomerSendToDateAndTime();
            LocalDateTime currentDateTime = LocalDateTime.now();
            if (sendToCustomerDateTime != null && sendToCustomerDateTime.isBefore(currentDateTime.plusHours(1))) {
                validationMessages.append("Date and time to send to the customer must be at least 1 hour after the current date and time;");
            }
        }

        private void validateLiabilityAmounts(PowerSupplyDisconnectionReminderBaseRequest request, StringBuilder validationMessages) {
            BigDecimal liabilityAmountFrom = request.getLiabilityAmountFrom();
            BigDecimal liabilityAmountTo = request.getLiabilityAmountTo();
            if (liabilityAmountFrom != null && liabilityAmountTo != null && liabilityAmountFrom.compareTo(liabilityAmountTo) >= 0) {
                validationMessages.append("Liability amount from must be less than Liability amount to;");
            }
        }

        private void validateCurrency(PowerSupplyDisconnectionReminderBaseRequest request, StringBuilder validationMessages) {
            BigDecimal liabilityAmountFrom = request.getLiabilityAmountFrom();
            BigDecimal liabilityAmountTo = request.getLiabilityAmountTo();
            Long currencyId = request.getCurrencyId();
            if ((liabilityAmountFrom != null || liabilityAmountTo != null) && currencyId == null) {
                validationMessages.append("Currency is mandatory when Liability amount from or Liability amount to is defined;");
            }
        }

        private void validateSendToCustomerDateTimeAndLiabilitiesMaxDueDate(PowerSupplyDisconnectionReminderBaseRequest request, StringBuilder validationMessages) {
            LocalDateTime sendToCustomerDateTime = request.getCustomerSendToDateAndTime();
            LocalDate liabilitiesMaxDueDate = request.getLiabilitiesMaxDueDate();
            if (sendToCustomerDateTime != null && liabilitiesMaxDueDate != null && sendToCustomerDateTime.toLocalDate().isBefore(liabilitiesMaxDueDate)) {
                validationMessages.append("Date to send to the customer must be after the Liabilities max due date;");
            }
        }

        private void validateTemplates(PowerSupplyDisconnectionReminderBaseRequest request, StringBuilder violations) {
            List<CommunicationChannel> channels = request.getCommunicationChannels();
            if (channels == null) {
                return;
            }

            checkChannel(CommunicationChannel.SMS, channels, request.getSmsTemplateId(), violations, "smsTemplateId-");
            checkChannel(CommunicationChannel.EMAIL, channels, request.getEmailTemplateId(), violations, "emailTemplateId-");
            checkChannel(CommunicationChannel.ON_PAPER, channels, request.getDocumentTemplateId(), violations, "documentTemplateId-");
        }

        private void checkChannel(CommunicationChannel communicationChannel, List<CommunicationChannel> communicationChannels, Long id, StringBuilder violations, String fieldName) {
            if(communicationChannels.contains(communicationChannel) && id==null) {
                append(violations, fieldName + "is mandatory when communication channel is defined;");
            } else if(!communicationChannels.contains(communicationChannel) && id!=null) {
                append(violations, fieldName + "is disabled when communication channel is not defined;");
            }
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }
    }
}