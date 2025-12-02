package bg.energo.phoenix.model.customAnotations.billing.communicationData;

import bg.energo.phoenix.model.request.billing.communicationData.BillingCommunicationDataListRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BillingCommunicationDataValidator.BillingCommunicationDataValidatorImpl.class)
public @interface BillingCommunicationDataValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingCommunicationDataValidatorImpl implements ConstraintValidator<BillingCommunicationDataValidator, BillingCommunicationDataListRequest> {

        @Override
        public boolean isValid(BillingCommunicationDataListRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            if (request.getContractOrderType() != null && request.getContractOrderId() == null) {
                validationMessages.append("billingCommunicationDataRequest.contractOrderId-[contractOrderId] should not be null;");
            } else if (request.getContractOrderId() != null && request.getContractOrderType() == null) {
                validationMessages.append("billingCommunicationDataRequest.contractOrderType-[contractOrderType] should not be null;");
            }
            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

    }
}
