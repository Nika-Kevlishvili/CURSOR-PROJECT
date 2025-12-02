package bg.energo.phoenix.model.customAnotations.crm.emailCommuncation;

import bg.energo.phoenix.model.request.crm.emailCommunication.MassEmailCreateRequest;
import bg.energo.phoenix.model.request.crm.emailCommunication.MassEmailEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {
        MassEmailCommunicationRequestValidator.MassEmailCreateRequestValidatorImpl.class,
        MassEmailCommunicationRequestValidator.MassEmailEditRequestValidatorImpl.class
})
public @interface MassEmailCommunicationRequestValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MassEmailCreateRequestValidatorImpl implements ConstraintValidator<MassEmailCommunicationRequestValidator, MassEmailCreateRequest> {
        @Override
        public boolean isValid(MassEmailCreateRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateCustomerImport(request, violations);
            validateRelatedCustomers(request, violations);
            validateBodyAndTemplate(request, violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateRelatedCustomers(MassEmailCreateRequest request, StringBuilder violations) {
            if ((Objects.isNull(request.getCommunicationAsInstitution()) || !request.getCommunicationAsInstitution()) && CollectionUtils.isNotEmpty(request.getRelatedCustomerIds())) {
                append(violations, "relatedCustomerIds-relatedCustomerIds must be null when communication as an institution not selected;");
            }
        }

        private void validateBodyAndTemplate(MassEmailCreateRequest request, StringBuilder violations) {
            if (Objects.nonNull(request.getEmailBody()) && Objects.nonNull(request.getEmailTemplateId())) {
                append(violations, "emailTemplateId-emailTemplateId must be null when email body is filled;");
                append(violations, "emailBody-emailBody must be null when email template is selected;");
            }

            if (Objects.isNull(request.getEmailBody()) && Objects.isNull(request.getEmailTemplateId())) {
                append(violations, "emailTemplateId-emailTemplateId or email body must be selected;");
                append(violations, "emailBody-emailBody or emailTemplateI must be selected;");
            }
        }

        private void validateCustomerImport(MassEmailCreateRequest request, StringBuilder violations) {
            if (request.isAllCustomersWithActiveContract() && CollectionUtils.isNotEmpty(request.getCustomers())) {
                append(violations, "customers-customers must be null when all customers with active contract is selected;");
            }

            if (!request.isAllCustomersWithActiveContract() && CollectionUtils.isEmpty(request.getCustomers())) {
                append(violations, "customers-customers must not be null when all customers with active contract is not selected;");
            }
        }
    }

    class MassEmailEditRequestValidatorImpl implements ConstraintValidator<MassEmailCommunicationRequestValidator, MassEmailEditRequest> {
        @Override
        public boolean isValid(MassEmailEditRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            validateCustomerImport(request, violations);
            validateRelatedCustomers(request, violations);
            validateBodyAndTemplate(request, violations);

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }

        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

        private void validateRelatedCustomers(MassEmailEditRequest request, StringBuilder violations) {
            if ((Objects.isNull(request.getCommunicationAsInstitution()) || !request.getCommunicationAsInstitution()) && CollectionUtils.isNotEmpty(request.getRelatedCustomerIds())) {
                append(violations, "relatedCustomerIds-relatedCustomerIds must be null when communication as an institution not selected;");
            }
        }

        private void validateCustomerImport(MassEmailEditRequest request, StringBuilder violations) {
            if (request.isAllCustomersWithActiveContract() && CollectionUtils.isNotEmpty(request.getCustomers())) {
                append(violations, "customers-customers must be null when all customers with active contract is selected;");
            }

            if (!request.isAllCustomersWithActiveContract() && CollectionUtils.isEmpty(request.getCustomers())) {
                append(violations, "customers-customers must not be null when all customers with active contract is not selected;");
            }
        }

        private void validateBodyAndTemplate(MassEmailEditRequest request, StringBuilder violations) {
            if (Objects.nonNull(request.getEmailBody()) && Objects.nonNull(request.getEmailTemplateId())) {
                append(violations, "emailTemplateId-emailTemplateId must be null when email body is filled;");
                append(violations, "emailBody-emailBody must be null when email template is selected;");
            }

            if (Objects.isNull(request.getEmailBody()) && Objects.isNull(request.getEmailTemplateId())) {
                append(violations, "emailTemplateId-emailTemplateId or email body must be selected;");
                append(violations, "emailBody-emailBody or emailTemplateI must be selected;");
            }
        }
    }

}
