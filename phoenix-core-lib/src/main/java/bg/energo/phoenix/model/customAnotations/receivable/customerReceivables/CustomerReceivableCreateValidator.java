package bg.energo.phoenix.model.customAnotations.receivable.customerReceivables;

import bg.energo.phoenix.model.request.receivable.customerReceivable.CustomerReceivableRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = CustomerReceivableCreateValidator.CustomerReceivableCreateValidatorImpl.class)
public @interface CustomerReceivableCreateValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerReceivableCreateValidatorImpl implements ConstraintValidator<CustomerReceivableCreateValidator, CustomerReceivableRequest> {

        @Override
        public boolean isValid(CustomerReceivableRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            if (request != null) {
                if (request.isDirectDebit()) {
                    if (request.getBankId() == null) {
                        validationMessages.append("bankId-[bankId] bank id is mandatory;");
                    }
                    if (request.getBankAccount() == null) {
                        validationMessages.append("bankAccount-[bankAccount] bank account is mandatory;");
                    }

                }
                if (request.isBlockedForOffsetting()) {
                    if (request.getBlockedFromDate() != null && request.getBlockedToDate() != null && request.getBlockedToDate().isBefore(request.getBlockedFromDate())) {
                        validationMessages.append("if blockedForOffsetting is checked, blockedFromDate must be before blockedToDate;");
                    }
                    if (request.getBlockedFromDate() == null) {
                        validationMessages.append("when blockedForOffsetting is checked, blockedFromDate is mandatory;");
                    }
                    if (request.getReasonId() == null) {
                        validationMessages.append("reasonId-[reasonId] reason id is mandatory;");
                    }
                } else {
                    if (request.getBlockedFromDate() != null) {
                        validationMessages.append("blockedFromDate-[blockedFromDate] blocked from date is disabled;");
                    }
                    if (request.getBlockedToDate() != null) {
                        validationMessages.append("blockedToDate-[blockedToDate] blocked to date is disabled;");
                    }
                    if (request.getReasonId() != null) {
                        validationMessages.append("reasonId-[reasonId] reason id is disabled;");
                    }
                    if (request.getAdditionalInformation() != null) {
                        validationMessages.append("additionalInformation-[additionalInformation] additional information is disabled;");
                    }
                }

                if (request.getOccurrenceDate() != null && request.getDueDate() != null) {
                    if (request.getOccurrenceDate().isAfter(request.getDueDate())) {
                        validationMessages.append("occurrenceDate-occurrenceDate can not be after due date;");
                    }
                }
            }
            if (!validationMessages.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }
    }
}
