package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCommunicationContactRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCommunicationsRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressContractCommunicationDataValidator.ExpressContractCommunicationDataValidatorImpl.class})
public @interface ExpressContractCommunicationDataValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExpressContractCommunicationDataValidatorImpl implements ConstraintValidator<ExpressContractCommunicationDataValidator, ExpressContractCustomerRequest> {
        @Override
        public boolean isValid(ExpressContractCustomerRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            StringBuilder stringBuilder = new StringBuilder();
            List<ExpressContractCommunicationsRequest> communications = request.getCommunications();

            for (int i = 0; i < communications.size(); i++) {
                ExpressContractCommunicationsRequest communication = communications.get(i);
                List<ExpressContractCommunicationContactRequest> contactRequests = communication.getContactRequests();
                boolean containsEmail = false;
                boolean containsMobile = false;
                for (ExpressContractCommunicationContactRequest expressContractCommunicationContactRequest : contactRequests) {
                    CustomerCommContactTypes contactType = expressContractCommunicationContactRequest.getContactType();
                    if (contactType != null) {
                        if (contactType == CustomerCommContactTypes.EMAIL) {
                            containsEmail = true;
                        }
                        if (contactType == CustomerCommContactTypes.MOBILE_NUMBER) {
                            containsMobile = true;
                        }
                    }
                }
                if (!containsEmail) {
                    stringBuilder.append(String.format("customer.communications[%s]-contact person should contain email;", i));
                }
                if (!containsMobile) {
                    stringBuilder.append(String.format("customer.communications[%s]-contact person should contain mobile;", i));
                }
            }
            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return stringBuilder.isEmpty();
        }

    }
}
