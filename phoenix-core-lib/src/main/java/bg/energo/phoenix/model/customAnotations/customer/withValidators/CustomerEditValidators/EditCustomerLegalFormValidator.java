package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.BusinessCustomerDetails;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
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
@Constraint(validatedBy = EditCustomerLegalFormValidator.LegalFormValidatorImpl.class)
public @interface EditCustomerLegalFormValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    class LegalFormValidatorImpl implements ConstraintValidator<EditCustomerLegalFormValidator, EditCustomerRequest> {
        @Override
        public boolean isValid(EditCustomerRequest value, ConstraintValidatorContext context) {
            CustomerType customerType = value.getCustomerType();
            if(customerType==null){
                return true;
            }
            StringBuilder messages = new StringBuilder();
            if (customerType.equals(CustomerType.PRIVATE_CUSTOMER) && Boolean.TRUE.equals(!value.getBusinessActivity())) {
                return true;
            }
            BusinessCustomerDetails businessCustomerDetails = value.getBusinessCustomerDetails();
            if (businessCustomerDetails == null) {
                return true;
            }
            if (customerType.equals(CustomerType.LEGAL_ENTITY)) {
                if (businessCustomerDetails.getLegalFormId() == null) {
                    messages.append("businessCustomerDetails.legalFormId-Legal Form ID is required;");
                }
                if (businessCustomerDetails.getLegalFormTransId() == null) {
                    messages.append("businessCustomerDetails.legalFormTransId-Legal Form Transl. ID is required;");
                }
            }
            if(!messages.isEmpty()){
                context.buildConstraintViolationWithTemplate(messages.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
