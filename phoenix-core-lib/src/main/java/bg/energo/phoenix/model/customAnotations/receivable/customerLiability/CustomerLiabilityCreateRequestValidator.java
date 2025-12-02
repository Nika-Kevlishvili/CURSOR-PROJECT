package bg.energo.phoenix.model.customAnotations.receivable.customerLiability;


import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = CustomerLiabilityCreateRequestValidator.CustomerLiabilityCreateRequestValidatorImpl.class)
public @interface CustomerLiabilityCreateRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class CustomerLiabilityCreateRequestValidatorImpl implements ConstraintValidator<CustomerLiabilityCreateRequestValidator, CustomerLiabilityRequest> {
        @Override
        public boolean isValid(CustomerLiabilityRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateInterestDates(request, validationMessages);
            validateDirectDebit(request, validationMessages);
            validateBlockedForPayment(request, validationMessages);
            validateBlockedForReminderLetters(request, validationMessages);
            validateBlockedForCalculationOfLatePayment(request, validationMessages);
            validateBlockedForLiabilitiesOffsetting(request, validationMessages);
            validateBlockedForSupplyTermination(request, validationMessages);
            validateAmountWithoutInterest(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateInterestDates(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            LocalDate interestDateFrom = request.getInterestDateFrom();
            LocalDate interestDateTo = request.getInterestDateTo();
            if (interestDateFrom != null && interestDateTo != null && interestDateFrom.isAfter(interestDateTo)) {
                validationMessages.append("interestDateFrom must be less than or equal to interestDateTo;");
            }
        }

        private void validateDirectDebit(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isDirectDebit()) {
                if (request.getBankId() == null) {
                    validationMessages.append("bankId is mandatory when directDebit is true;");
                }
                if (request.getBankAccount() == null) {
                    validationMessages.append("bankAccount is mandatory when directDebit is true;");
                }
            }
        }

        private void validateBlockedForPayment(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isBlockedForPayment()) {
                validateBlockedDates(request.getBlockedForPaymentFromDate(), request.getBlockedForPaymentToDate(), "blockedForPayment", validationMessages);
                validateBlockedReason(request.getBlockedForPaymentReasonId(), "blockedForPayment", validationMessages);
            } else {
                validateBlockedFieldsDisabled(request.getBlockedForPaymentFromDate(), request.getBlockedForPaymentToDate(),
                        request.getBlockedForPaymentReasonId(), request.getBlockedForPaymentAdditionalInfo(), "blockedForPayment", validationMessages);
            }
        }

        private void validateBlockedForReminderLetters(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isBlockedForReminderLetters()) {
                validateBlockedDates(request.getBlockedForReminderLettersFromDate(), request.getBlockedForReminderLettersToDate(), "blockedForReminderLetters", validationMessages);
                validateBlockedReason(request.getBlockedForReminderLettersReasonId(), "blockedForReminderLetters", validationMessages);
            } else {
                validateBlockedFieldsDisabled(request.getBlockedForReminderLettersFromDate(), request.getBlockedForReminderLettersToDate(),
                        request.getBlockedForReminderLettersReasonId(), request.getBlockedForReminderLettersAdditionalInfo(), "blockedForReminderLetters", validationMessages);
            }
        }

        private void validateBlockedForCalculationOfLatePayment(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isBlockedForCalculationOfLatePayment()) {
                validateBlockedDates(request.getBlockedForCalculationOfLatePaymentFromDate(), request.getBlockedForCalculationOfLatePaymentToDate(), "blockedForCalculationOfLatePayment", validationMessages);
                validateBlockedReason(request.getBlockedForCalculationOfLatePaymentReasonId(), "blockedForCalculationOfLatePayment", validationMessages);
            } else {
                validateBlockedFieldsDisabled(request.getBlockedForCalculationOfLatePaymentFromDate(), request.getBlockedForCalculationOfLatePaymentToDate(),
                        request.getBlockedForCalculationOfLatePaymentReasonId(), request.getBlockedForCalculationOfLatePaymentAdditionalInfo(), "blockedForCalculationOfLatePayment", validationMessages);
            }
        }

        private void validateBlockedForLiabilitiesOffsetting(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isBlockedForLiabilitiesOffsetting()) {
                validateBlockedDates(request.getBlockedForLiabilitiesOffsettingFromDate(), request.getBlockedForLiabilitiesOffsettingToDate(), "blockedForLiabilitiesOffsetting", validationMessages);
                validateBlockedReason(request.getBlockedForLiabilitiesOffsettingReasonId(), "blockedForLiabilitiesOffsetting", validationMessages);
            } else {
                validateBlockedFieldsDisabled(request.getBlockedForLiabilitiesOffsettingFromDate(), request.getBlockedForLiabilitiesOffsettingToDate(),
                        request.getBlockedForLiabilitiesOffsettingReasonId(), request.getBlockedForLiabilitiesOffsettingAdditionalInfo(), "blockedForLiabilitiesOffsetting", validationMessages);
            }
        }

        private void validateBlockedForSupplyTermination(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            if (request.isBlockedForSupplyTermination()) {
                validateBlockedDates(request.getBlockedForSupplyTerminationFromDate(), request.getBlockedForSupplyTerminationToDate(), "blockedForSupplyTermination", validationMessages);
                validateBlockedReason(request.getBlockedForSupplyTerminationReasonId(), "blockedForSupplyTermination", validationMessages);
            } else {
                validateBlockedFieldsDisabled(request.getBlockedForSupplyTerminationFromDate(), request.getBlockedForSupplyTerminationToDate(),
                        request.getBlockedForSupplyTerminationReasonId(), request.getBlockedForSupplyTerminationAdditionalInfo(), "blockedForSupplyTermination", validationMessages);
            }
        }

        private void validateBlockedDates(LocalDate fromDate, LocalDate toDate, String fieldPrefix, StringBuilder validationMessages) {
            if (fromDate == null) {
                validationMessages.append(fieldPrefix).append("FromDate is mandatory;");
            }

            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                validationMessages.append(fieldPrefix).append("FromDate must be less than ").append(fieldPrefix).append("ToDate;");
            }
        }

        private void validateBlockedReason(Long reasonId, String fieldPrefix, StringBuilder validationMessages) {
            if (reasonId == null) {
                validationMessages.append(fieldPrefix).append("ReasonId is mandatory;");
            }
        }

        private void validateBlockedFieldsDisabled(LocalDate fromDate, LocalDate toDate, Long reasonId, String additionalInfo, String fieldPrefix, StringBuilder validationMessages) {
            if (fromDate != null) {
                validationMessages.append(fieldPrefix).append("FromDate must be null when ").append(fieldPrefix).append(" is false;");
            }
            if (toDate != null) {
                validationMessages.append(fieldPrefix).append("ToDate must be null when ").append(fieldPrefix).append(" is false;");
            }
            if (reasonId != null) {
                validationMessages.append(fieldPrefix).append("ReasonId must be null when ").append(fieldPrefix).append(" is false;");
            }
            if (additionalInfo != null) {
                validationMessages.append(fieldPrefix).append("AdditionalInfo must be null when ").append(fieldPrefix).append(" is false;");
            }
        }

        private void validateAmountWithoutInterest(CustomerLiabilityRequest request, StringBuilder validationMessages) {
            BigDecimal amountWithoutInterest = request.getAmountWithoutInterest();
            BigDecimal initialAmount = request.getInitialAmount();

            if (amountWithoutInterest != null && initialAmount != null) {
                if (amountWithoutInterest.compareTo(initialAmount) > 0) {
                    validationMessages.append("amountWithoutInterest must be less than or equal to initialAmount;");
                }
            }
        }
    }
}