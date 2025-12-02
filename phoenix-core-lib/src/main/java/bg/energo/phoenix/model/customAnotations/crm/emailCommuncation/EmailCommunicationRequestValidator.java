package bg.energo.phoenix.model.customAnotations.crm.emailCommuncation;

import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.request.crm.emailCommunication.EmailCommunicationCreateRequest;
import bg.energo.phoenix.model.request.crm.emailCommunication.EmailCommunicationEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {
        EmailCommunicationRequestValidator.EmailCommunicationRequestValidatorImpl.class,
        EmailCommunicationRequestValidator.EmailCommunicationEditRequestValidatorImpl.class
})
public @interface EmailCommunicationRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EmailCommunicationRequestValidatorImpl implements ConstraintValidator<EmailCommunicationRequestValidator, EmailCommunicationCreateRequest> {
        @Override
        public boolean isValid(EmailCommunicationCreateRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateDateAndTime(request, violations);
            validateEmailAddress(request, violations);
            validateRelatedCustomers(request, violations);
            validateBodyAndTemplate(request.getEmailBody(), request.getEmailTemplateId(), violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateDateAndTime(EmailCommunicationCreateRequest request, StringBuilder violations) {
            EmailCommunicationType emailCommunicationType = request.getEmailCommunicationType();
            if (Objects.nonNull(emailCommunicationType)) {
                if (emailCommunicationType.equals(EmailCommunicationType.OUTGOING) && Objects.nonNull(request.getDateTime())) {
                    append(violations, "dateTime-[dateTime] should be null or empty when email communication type is OUTGOING;");
                }
                if (emailCommunicationType.equals(EmailCommunicationType.INCOMING) && Objects.isNull(request.getDateTime())) {
                    append(violations, "dateTime-[dateTime] should not be null or empty when email communication type is INCOMING;");
                }
            }
        }

        private void validateEmailAddress(EmailCommunicationCreateRequest request, StringBuilder violations) {
            if (Objects.nonNull(request.getCustomerEmailAddress())) {
                Set<String> emails = splitEmail(request.getCustomerEmailAddress());
                Pattern pattern = Pattern.compile("^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+$");
                emails.forEach(s -> {
                    Matcher matcher = pattern.matcher(s);
                    if (!matcher.matches()) {
                        append(violations, "customerEmailAddress-customerEmailAddress invalid email format;");
                    }
                    if (s.length() > 128) {
                        append(violations, "customerEmailAddress-customerEmailAddress max symbols for each email address is 128;");
                    }
                });
            }
        }

        private void validateRelatedCustomers(EmailCommunicationCreateRequest request, StringBuilder violations) {
            if ((Objects.isNull(request.getCommunicationAsAnInstitution()) || !request.getCommunicationAsAnInstitution()) && CollectionUtils.isNotEmpty(request.getRelatedCustomerIds())) {
                append(violations, "relatedCustomerIds-relatedCustomerIds must be null when communication as an institution not selected;");
            }
        }

        private void validateBodyAndTemplate(String body, Long templateId, StringBuilder violations) {
            if (Objects.nonNull(body) && Objects.nonNull(templateId)) {
                append(violations, "emailTemplateId-emailTemplateId must be null when email body is filled;");
                append(violations, "emailBody-emailBody must be null when email template is selected;");
            }

            if (Objects.isNull(body) && Objects.isNull(templateId)) {
                append(violations, "emailTemplateId-emailTemplateId or email body must be selected;");
                append(violations, "emailBody-emailBody or emailTemplateI must be selected;");
            }
        }

        private Set<String> splitEmail(String emailAddress) {
            if (emailAddress.contains(";")) {
                return Arrays
                        .stream(emailAddress.split(";"))
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .collect(Collectors.toSet());
            } else {
                return Set.of(emailAddress);
            }
        }
    }

    class EmailCommunicationEditRequestValidatorImpl implements ConstraintValidator<EmailCommunicationRequestValidator, EmailCommunicationEditRequest> {
        @Override
        public boolean isValid(EmailCommunicationEditRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateDateAndTime(request, violations);
            validateEmailAddress(request, violations);
            validateRelatedCustomers(request, violations);
            validateBodyAndTemplate(request.getEmailBody(), request.getEmailTemplateId(), violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateDateAndTime(EmailCommunicationEditRequest request, StringBuilder violations) {
            EmailCommunicationType emailCommunicationType = request.getEmailCommunicationType();
            if (Objects.nonNull(emailCommunicationType)) {
                if (emailCommunicationType.equals(EmailCommunicationType.OUTGOING) && Objects.nonNull(request.getDateTime())) {
                    append(violations, "dateTime-[dateTime] should be null or empty when email communication type is OUTGOING;");
                }
                if (emailCommunicationType.equals(EmailCommunicationType.INCOMING) && Objects.isNull(request.getDateTime())) {
                    append(violations, "dateTime-[dateTime] should not be null or empty when email communication type is INCOMING;");
                }
            }
        }

        private void validateEmailAddress(EmailCommunicationEditRequest request, StringBuilder violations) {
            if (Objects.nonNull(request.getCustomerEmailAddress())) {
                Set<String> emails = splitEmail(request.getCustomerEmailAddress());
                Pattern pattern = Pattern.compile("^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+$");
                emails.forEach(s -> {
                    Matcher matcher = pattern.matcher(s);
                    if (!matcher.matches()) {
                        append(violations, "customerEmailAddress-customerEmailAddress invalid email format;");
                    }
                    if (s.length() > 128) {
                        append(violations, "customerEmailAddress-customerEmailAddress max symbols for each email address is 128;");
                    }
                });
            }
        }

        private void validateRelatedCustomers(EmailCommunicationEditRequest request, StringBuilder violations) {
            if ((Objects.isNull(request.getCommunicationAsAnInstitution()) || !request.getCommunicationAsAnInstitution()) && CollectionUtils.isNotEmpty(request.getRelatedCustomerIds())) {
                append(violations, "relatedCustomerIds-relatedCustomerIds must be null when communication as an institution not selected;");
            }
        }

        private void validateBodyAndTemplate(String body, Long templateId, StringBuilder violations) {
            if (Objects.nonNull(body) && Objects.nonNull(templateId)) {
                append(violations, "emailTemplateId-emailTemplateId must be null when email body is filled;");
                append(violations, "emailBody-emailBody must be null when email template is selected;");
            }

            if (Objects.isNull(body) && Objects.isNull(templateId)) {
                append(violations, "emailTemplateId-emailTemplateId or email body must be selected;");
                append(violations, "emailBody-emailBody or emailTemplateI must be selected;");
            }
        }

        private Set<String> splitEmail(String emailAddress) {
            if (emailAddress.contains(";")) {
                return Arrays
                        .stream(emailAddress.split(";"))
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .collect(Collectors.toSet());
            } else {
                return Set.of(emailAddress);
            }
        }
    }

}

