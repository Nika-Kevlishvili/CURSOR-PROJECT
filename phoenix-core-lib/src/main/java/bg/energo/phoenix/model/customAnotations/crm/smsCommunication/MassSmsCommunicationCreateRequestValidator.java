package bg.energo.phoenix.model.customAnotations.crm.smsCommunication;

import bg.energo.phoenix.model.request.crm.smsCommunication.MassSmsCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = MassSmsCommunicationCreateRequestValidator.MassSmsCommunicationCreateRequestValidatorImpl.class)

public @interface MassSmsCommunicationCreateRequestValidator {
    String value() default "";
    String message() default "";
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MassSmsCommunicationCreateRequestValidatorImpl implements ConstraintValidator<MassSmsCommunicationCreateRequestValidator, MassSmsCreateRequest> {

        @Override
        public boolean isValid(MassSmsCreateRequest request, ConstraintValidatorContext constraintValidatorContext) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            if(!request.isCommunicationAsInstitution()) {
                if(request.getRelatedCustomerIds()!=null && !request.getRelatedCustomerIds().isEmpty()) {
                    validationMessages.append("relatedCustomerIds-[relatedCustomerIds] related customer ids is disabled when communication as institution is not checked!;");
                }
            }

            if(request.getSmsBody()!=null && request.getTemplateId()!=null) {
                validationMessages.append("smsBody-[smsBody] and templateId-[templateId] are not allowed to be set together!;");
            } else if(request.getSmsBody()==null && request.getTemplateId()==null) {
                validationMessages.append("smsBody-[smsBody] and templateId-[templateId] one of them must be set!;");
            } else if(request.getSmsBody()!=null && request.getSmsBody().isEmpty()) {
                validationMessages.append("sms Body must not be empty!;");
            }

            if(request.isAllCustomersWithActiveContract() && (request.getCustomers()!=null  && !request.getCustomers().isEmpty())) {
                validationMessages.append("customers-[customers] customers are not allowed when all customers with active contract is checked!;");
            } else if(!request.isAllCustomersWithActiveContract() && (request.getCustomers()==null || request.getCustomers().isEmpty())) {
                validationMessages.append("customers-[customers] must not be empty!;");
            }
            if (!validationMessages.isEmpty()) {
                isValid = false;
                constraintValidatorContext.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
