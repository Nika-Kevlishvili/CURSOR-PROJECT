package bg.energo.phoenix.model.customAnotations.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.*;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ReceivableBlockingBaseRequestValidator.ReceivableBlockingCreateRequestValidatorImpl.class)
public @interface ReceivableBlockingBaseRequestValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ReceivableBlockingCreateRequestValidatorImpl implements ConstraintValidator<ReceivableBlockingBaseRequestValidator, ReceivableBlockingBaseRequest> {
        @Override
        public boolean isValid(ReceivableBlockingBaseRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateByConditionType(request, violations);
            validateBlockingForPayment(request.getIsBlockForPayment(), request.getBlockingForPayment(), violations);
            validateBlockingForCalculation(request.getIsBlockForCalculation(), request.getBlockingForCalculation(), violations);
            validateBlockingForLiabilities(request.getIsBlockForLiabilitiesOffsetting(), request.getBlockingForLiabilitiesOffsetting(), violations);
            validateBlockingForRemindLetters(request.getIsBlockForReminderLetters(), request.getBlockingForReminderLetters(), violations);
            validateBlockingForSupplyTermination(request.getIsBlockForSupplyTermination(), request.getBlockingForSupplyTermination(), violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }


        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateByConditionType(ReceivableBlockingBaseRequest request, StringBuilder violations) {
            ReceivableBlockingConditionType receivableBlockingConditionType = request.getReceivableBlockingConditionType();
            if (receivableBlockingConditionType != null) {
                switch (receivableBlockingConditionType) {
                    case ALL_CUSTOMERS -> {
                        if (Objects.nonNull(request.getListOfCustomers())) {
                            append(violations, "listOfCustomers-should be null when all customers is selected;");
                        }

                        if (Objects.nonNull(request.getConditions())) {
                            append(violations, "conditions-should be null when all customers is selected;");
                        }
                    }
                    case CUSTOMERS_UNDER_CONDITIONS -> {
                        if (Objects.nonNull(request.getListOfCustomers())) {
                            append(violations, "listOfCustomers-should be null when customer under conditions is selected;");
                        }

                        if (Objects.isNull(request.getConditions())) {
                            append(violations, "conditions-should not be null when customer under conditions is selected;");
                        }
                    }
                    case LIST_OF_CUSTOMERS -> {
                        if (Objects.nonNull(request.getConditions())) {
                            append(violations, "conditions-should be null when list of customers is selected;");
                        }

                        if (Objects.isNull(request.getListOfCustomers())) {
                            append(violations, "listOfCustomers-should not be null when list of customers is selected;");
                        }
                    }
                }
            }
        }

        private void validateBlockingForPayment(Boolean isSelected, BlockingForPaymentRequest request, StringBuilder violations) {
            if (Objects.isNull(isSelected) || !isSelected) {
                if (Objects.nonNull(request)) {
                    append(violations, "blockingForPayment-blocking for payment must be null when blocked for payment checkbox is not selected;");
                }
            }

            if (Objects.nonNull(isSelected) && isSelected) {
                if (Objects.isNull(request)) {
                    append(violations, "blockingForPayment-blocking for payment must not be null when blocked for payment checkbox is selected;");
                }
            }

            if (request != null) {
                if (Objects.nonNull(request.getReasonType()) && !request.getReasonType().equals(ReceivableBlockingReasonType.BLOCKED_FOR_PAYMENT)) {
                    append(violations, "blockingForPayment.reasonType-reason type must be blocked for payment when blocked for payment checkbox is selected;");
                }

                LocalDate fromDate = request.getFromDate();
                LocalDate toDate = request.getToDate();

                if (Objects.nonNull(fromDate) && Objects.nonNull(toDate) && fromDate.isAfter(toDate)) {
                    append(violations, "blockingForPayment.fromDate-fromDate can not be after toDate;");
                }
            }

        }

        private void validateBlockingForCalculation(Boolean isSelected, BlockingForCalculationRequest request, StringBuilder violations) {
            if (Objects.isNull(isSelected) || !isSelected) {
                if (Objects.nonNull(request)) {
                    append(violations, "blockingForCalculation-blocking for calculation must be null when blocked for calculation checkbox is not selected;");
                }
            }

            if (Objects.nonNull(isSelected) && isSelected) {
                if (Objects.isNull(request)) {
                    append(violations, "blockingForCalculation-blocking for calculation must not be null when blocked for calculation checkbox is selected;");
                }
            }

            if (request != null) {
                if (Objects.nonNull(request.getReasonType()) && !request.getReasonType().equals(ReceivableBlockingReasonType.BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS)) {
                    append(violations, "blockingForCalculation.reasonType-reason type must be blocked for calculation when blocked for calculation checkbox is selected;");
                }

                LocalDate fromDate = request.getFromDate();
                LocalDate toDate = request.getToDate();

                if (Objects.nonNull(fromDate) && Objects.nonNull(toDate) && fromDate.isAfter(toDate)) {
                    append(violations, "blockingForCalculation.fromDate-fromDate can not be after toDate;");
                }
            }

        }

        private void validateBlockingForLiabilities(Boolean isSelected, BlockingForLiabilitiesOffsettingRequest request, StringBuilder violations) {
            if (Objects.isNull(isSelected) || !isSelected) {
                if (Objects.nonNull(request)) {
                    append(violations, "blockingForLiabilitiesOffsetting-blocking for liabilities must be null when blocked for liabilities checkbox is not selected;");
                }
            }

            if (Objects.nonNull(isSelected) && isSelected) {
                if (Objects.isNull(request)) {
                    append(violations, "blockingForLiabilitiesOffsetting-blocking for liabilities must not be null when blocked for liabilities checkbox is selected;");
                }
            }

            if (request != null) {
                if (Objects.nonNull(request.getReasonType()) && !request.getReasonType().equals(ReceivableBlockingReasonType.BLOCKED_FOR_LIABILITIES_OFFSETTING)) {
                    append(violations, "blockingForLiabilitiesOffsetting.reasonType-reason type must be blocked for liabilities when blocked for liabilities checkbox is selected;");
                }

                LocalDate fromDate = request.getFromDate();
                LocalDate toDate = request.getToDate();

                if (Objects.nonNull(fromDate) && Objects.nonNull(toDate) && fromDate.isAfter(toDate)) {
                    append(violations, "blockingForLiabilitiesOffsetting.fromDate-fromDate can not be after toDate;");
                }
            }

        }

        private void validateBlockingForRemindLetters(Boolean isSelected, BlockingForReminderLettersRequest request, StringBuilder violations) {
            if (Objects.isNull(isSelected) || !isSelected) {
                if (Objects.nonNull(request)) {
                    append(violations, "blockingForReminderLetters-blocking for remind letters must be null when blocked for remind letters checkbox is not selected;");
                }
            }

            if (Objects.nonNull(isSelected) && isSelected) {
                if (Objects.isNull(request)) {
                    append(violations, "blockingForReminderLetters-blocking for remind letters must not be null when blocked for remind letters checkbox is selected;");
                }
            }

            if (request != null) {
                if (Objects.nonNull(request.getReasonType()) && !request.getReasonType().equals(ReceivableBlockingReasonType.BLOCKED_FOR_REMINDER_LETTERS)) {
                    append(violations, "blockingForReminderLetters.reasonType-reason type must be blocked for remind letters when blocked for remind letters checkbox is selected;");
                }

                LocalDate fromDate = request.getFromDate();
                LocalDate toDate = request.getToDate();

                if (Objects.nonNull(fromDate) && Objects.nonNull(toDate) && fromDate.isAfter(toDate)) {
                    append(violations, "blockingForReminderLetters.fromDate-fromDate can not be after toDate;");
                }
            }

        }

        private void validateBlockingForSupplyTermination(Boolean isSelected, BlockingForSupplyTerminationRequest request, StringBuilder violations) {
            if (Objects.isNull(isSelected) || !isSelected) {
                if (Objects.nonNull(request)) {
                    append(violations, "blockingForSupplyTermination-blocking for supply termination must be null when blocked for supply termination checkbox is not selected;");
                }
            }

            if (Objects.nonNull(isSelected) && isSelected) {
                if (Objects.isNull(request)) {
                    append(violations, "blockingForSupplyTermination-blocking for supply termination must not be null when blocked for supply termination checkbox is selected;");
                }
            }

            if (request != null) {
                if (Objects.nonNull(request.getReasonType()) && !request.getReasonType().equals(ReceivableBlockingReasonType.BLOCKED_FOR_SUPPLY_TERMINATION)) {
                    append(violations, "blockingForSupplyTermination.reasonType-reason type must be blocked for supply termination when blocked for supply termination checkbox is selected;");
                }

                LocalDate fromDate = request.getFromDate();
                LocalDate toDate = request.getToDate();

                if (Objects.nonNull(fromDate) && Objects.nonNull(toDate) && fromDate.isAfter(toDate)) {
                    append(violations, "blockingForSupplyTermination.fromDate-fromDate can not be after toDate;");
                }
            }

        }

    }

}
