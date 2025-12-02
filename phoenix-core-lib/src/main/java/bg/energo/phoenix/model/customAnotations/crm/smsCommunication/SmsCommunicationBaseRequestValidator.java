package bg.energo.phoenix.model.customAnotations.crm.smsCommunication;

import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationSave;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.request.crm.smsCommunication.SmsCommunicationBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = SmsCommunicationBaseRequestValidator.SmsCommunicationBaseRequestValidatorImpl.class)
public @interface SmsCommunicationBaseRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SmsCommunicationBaseRequestValidatorImpl implements ConstraintValidator<SmsCommunicationBaseRequestValidator, SmsCommunicationBaseRequest> {

        @Override
        public boolean isValid(SmsCommunicationBaseRequest request, ConstraintValidatorContext constraintValidatorContext) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            if (request.getCommunicationType().equals(CommunicationType.OUTGOING)) {
                if (request.getDateAndTime() != null) {
                    validationMessages.append("dateAndTime-[dateAndTime] date and time is disabled when communication type is outgoing!;");
                }
                if (request.getSaveAs() != CommunicationSave.SAVE_AS_DRAFT && request.getSaveAs() != CommunicationSave.SAVE_AND_SEND) {
                    validationMessages.append("saveAs-[saveAs] just save is disabled on when communication type is outgoing!;");
                }

                if (request.getSmsBody() != null && request.getTemplateId() != null) {
                    validationMessages.append("smsBody-[smsBody] and templateId-[templateId] are not allowed to be set together!;");
                } else if (request.getSmsBody() == null && request.getTemplateId() == null) {
                    validationMessages.append("smsBody-[smsBody] and templateId-[templateId] one of them must be set!;");
                } else if (request.getSmsBody() != null && request.getSmsBody().isEmpty()) {
                    validationMessages.append("sms Body must not be empty!;");
                }

            } else {
                if (request.getDateAndTime() == null) {
                    validationMessages.append("dateAndTime-[dateAndTime] date and time is mandatory when communication type is incoming!;");
                }
                if (request.getSaveAs() != CommunicationSave.JUST_SAVE) {
                    validationMessages.append("saveAs-[saveAs] save as draft and save and send is disabled when communication type is incoming!;");
                }
                if (request.getSmsBody() == null) {
                    validationMessages.append("smsBody-[smsBody] is mandatory when communication type is incoming!;");
                }
            }

            if (!request.isCommunicationAsInstitution()) {
                if (request.getRelatedCustomerIds() != null && !request.getRelatedCustomerIds().isEmpty()) {
                    validationMessages.append("relatedCustomerIds-[relatedCustomerIds] related customer ids is disabled when communication as institution is not checked!;");
                }
            }


            if (!validationMessages.isEmpty()) {
                isValid = false;
                constraintValidatorContext.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
            }
            return isValid;
        }
    }
}
