package bg.energo.phoenix.model.customAnotations.receivable.reminder;

import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.request.receivable.reminder.ReminderBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ReminderBaseValidator.ReminderBaseValidatorImpl.class)
public @interface ReminderBaseValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ReminderBaseValidatorImpl implements ConstraintValidator<ReminderBaseValidator, ReminderBaseRequest> {
        @Override
        public boolean isValid(ReminderBaseRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateByConditionType(request, violations);
            validateCurrency(request, violations);
            validatePrefixes(request, violations);
            validateTemplates(request, violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateByConditionType(ReminderBaseRequest request, StringBuilder violations) {
            ReminderConditionType reminderConditionType = request.getConditionType();
            if (reminderConditionType != null) {
                switch (reminderConditionType) {
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

        private void validateCurrency(ReminderBaseRequest request, StringBuilder violations) {
            BigDecimal dueAmountFrom = request.getDueAmountFrom();
            BigDecimal dueAmountTo = request.getDueAmountTo();
            Long currencyId = request.getCurrencyId();

            if (Objects.isNull(dueAmountFrom) && Objects.isNull(dueAmountTo) && Objects.nonNull(currencyId)) {
                append(violations, "currencyId-should be null when neither due amount from or due amount to is defined;");
            }

            if (Objects.nonNull(dueAmountFrom) || Objects.nonNull(dueAmountTo)) {
                if (Objects.isNull(currencyId)) {
                    append(violations, "currencyId-should not be null when due amount from or due amount to is defined;");
                }
            }

            //To do fix
            if (Objects.nonNull(dueAmountFrom) && Objects.nonNull(dueAmountTo)) {
                if (dueAmountTo.compareTo(dueAmountFrom) < 0) {
                    append(violations, "dueAmountTo-due amount to must be <= Greater than;");
                }
            }
        }

        private void validatePrefixes(ReminderBaseRequest request, StringBuilder violations) {
            List<Long> excludePrefixes = request.getExcludeLiabilitiesByPrefixes();
            List<Long> onlyPrefixes = request.getOnlyLiabilitiesWithPrefixes();

            if (CollectionUtils.isNotEmpty(excludePrefixes) && CollectionUtils.isNotEmpty(onlyPrefixes)) {
                append(violations, "excludeLiabilitiesByPrefixes-should be empty when only liabilities with prefixes is defined;");
                append(violations, "onlyLiabilitiesWithPrefixes-should be empty when exclude liabilities by prefixes is defined;");
            }
        }

        private void validateTemplates(ReminderBaseRequest request, StringBuilder violations) {
            checkChannel(CommunicationChannel.SMS,request.getCommunicationChannels(),request.getSmsTemplateId(),violations,"smsTemplateId-");
            checkChannel(CommunicationChannel.EMAIL,request.getCommunicationChannels(),request.getEmailTemplateId(),violations,"emailTemplateId-");
            checkChannel(CommunicationChannel.ON_PAPER,request.getCommunicationChannels(),request.getDocumentTemplateId(),violations,"documentTemplateId-");
        }

        private void checkChannel(CommunicationChannel communicationChannel,List<CommunicationChannel> communicationChannels,Long id,StringBuilder violations,String fieldName) {
            if(communicationChannels.contains(communicationChannel) && id==null) {
                append(violations, fieldName + "is mandatory when communication channel is defined;");
            } else if(!communicationChannels.contains(communicationChannel) && id!=null) {
                append(violations, fieldName + "is disabled when communication channel is not defined;");
            }
        }


    }

}
