package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressCustomerTypeValidator.ExpressCustomerTypeValidatorImpl.class})
public @interface ExpressCustomerTypeValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExpressCustomerTypeValidatorImpl implements ConstraintValidator<ExpressCustomerTypeValidator, ExpressContractCustomerRequest> {

        @Override
        public boolean isValid(ExpressContractCustomerRequest request, ConstraintValidatorContext context) {
            if(request.getCustomerType() == null){
                return false;
            }
            boolean correct = true;
            StringBuilder stringBuilder = new StringBuilder();
            context.disableDefaultConstraintViolation();
            if(request.getBusinessActivity() == null)
                request.setBusinessActivity(false);
            if(request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER
                    && (request.getBusinessActivity() != null && !request.getBusinessActivity())){
                if (request.getOwnershipFormId() != null) {
                    stringBuilder.append("customer.ownershipFormId-Form of Ownership must not be provided;");
                    correct = false;
                }

                if (request.getEconomicBranchCiId() != null) {
                    stringBuilder.append("customer.economicBranchId-Economic Branch must not be provided;");
                    correct = false;
                }


                if (request.getMainActivitySubject() != null) {
                    stringBuilder.append("customer.mainSubjectOfActivity-Main Subject of Activity must not be provided;");
                    correct = false;
                }

                if(request.getBusinessCustomerDetails() != null){
                    stringBuilder.append("customer.businessCustomerDetails-Business Customer Details must not be provided;");
                    correct = false;
                }

            }else{
                if(request.getBusinessCustomerDetails() == null){
                    stringBuilder.append("customer.businessCustomerDetails-Business Customer Details is required;");
                    correct = false;
                }
//                if(request.getManagerRequests()==null && request.getBusinessCustomerDetails() != null){
//                    stringBuilder.append("customer.managerRequest-Manager request must be provided;");
//                    correct = false;
//                }
            }

            if(request.getCustomerType() == CustomerType.LEGAL_ENTITY){
                if(!(request.getBusinessActivity() == null || Boolean.FALSE.equals(request.getBusinessActivity()))){
                    stringBuilder.append("customer.businessActivity-Business Activity must not be provided;");
                    correct = false;
                }
                if(request.getPrivateCustomerDetails() != null){
                    stringBuilder.append("customer.privateCustomerDetails-Private Customer Details must not be provided;");
                    correct = false;
                }

            }else{
                if(request.getBusinessActivity() == null){
                    stringBuilder.append("customer.businessActivity-Business Activity is required;");
                    correct = false;
                }
                if(request.getPrivateCustomerDetails() == null){
                    stringBuilder.append("customer.privateCustomerDetails-Private Customer Details is required;");
                    correct = false;
                }
            }

            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return correct;
        }

    }

}
