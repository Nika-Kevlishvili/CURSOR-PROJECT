package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.CreateCustomerRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/*
    Validate fields based on customer Types
    IF PRIVATE_CUSTOMER following fields must not be provided: Form of Ownership; Economic Branch;
    Economic Branch NCEA; Main Subject of Activity; Business Customer Details;
    and must be provided Private Customer Details
    IF LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY must be provided Business Customer details
 */
public class ValidationsByCustomerTypeImpl
        implements ConstraintValidator<ValidationsByCustomerType, CreateCustomerRequest> {
    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        if(request.getCustomerType() == null){
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        context.disableDefaultConstraintViolation();
        if(request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER){
            if (request.getOwnershipFormId() != null) {
                stringBuilder.append("Form of Ownership must not be provided; ");
                correct = false;
            }

            if (request.getEconomicBranchId() != null) {
                stringBuilder.append("Economic Branch must not be provided; ");
                correct = false;
            }

            if (request.getEconomicBranchNCEAId() != null) {
                stringBuilder.append("Economic Branch NCEA must not be provided; ");
                correct = false;
            }

            if (request.getMainSubjectOfActivity() != null) {
                stringBuilder.append("Main Subject of Activity must not be provided; ");
                correct = false;
            }

            if(request.getBusinessCustomerDetails() != null){
                stringBuilder.append("Business Customer Details must not be provided; ");
                correct = false;
            }

        }else{
            if(request.getBusinessCustomerDetails() == null){
                stringBuilder.append("Business Customer Details is required; ");
                correct = false;
            }
        }

        if(request.getCustomerType() == CustomerType.LEGAL_ENTITY){
            if(request.getPrivateCustomerDetails() != null){
                stringBuilder.append("Private Customer Details must not be provided; ");
                correct = false;
            }
        }else{
            if(request.getPrivateCustomerDetails() == null){
                stringBuilder.append("Private Customer Details is required; ");
                correct = false;
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
