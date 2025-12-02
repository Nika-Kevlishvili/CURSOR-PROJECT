package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EditValidationsByCustomerTypeImpl
        implements ConstraintValidator<EditValidationsByCustomerType, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest request, ConstraintValidatorContext context) {
        if (request.getCustomerType() == null) {
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        context.disableDefaultConstraintViolation();
        if (request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER
            && (request.getBusinessActivity() != null && !request.getBusinessActivity())) {
            if (request.getOwnershipFormId() != null) {
                stringBuilder.append("ownershipFormId-Form of Ownership must not be provided;");
                correct = false;
            }

            if (request.getEconomicBranchId() != null) {
                stringBuilder.append("economicBranchId-Economic Branch must not be provided;");
                correct = false;
            }

            if (request.getEconomicBranchNCEAId() != null) {
                stringBuilder.append("economicBranchNCEAId-Economic Branch NCEA must not be provided;");
                correct = false;
            }

            if (request.getMainSubjectOfActivity() != null) {
                stringBuilder.append("mainSubjectOfActivity-Main Subject of Activity must not be provided;");
                correct = false;
            }

            if (request.getBusinessCustomerDetails() != null) {
                stringBuilder.append("businessCustomerDetails-Business Customer Details must not be provided;");
                correct = false;
            }

        } else {
            if (request.getBusinessCustomerDetails() == null) {
                stringBuilder.append("businessCustomerDetails-Business Customer Details is required;");
                correct = false;
            }
        }

        if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
            if(request.getBusinessActivity() != null){
                stringBuilder.append("businessActivity-Business Activity must not be provided;");
                correct = false;
            }
            if (request.getPrivateCustomerDetails() != null) {
                stringBuilder.append("privateCustomerDetails-Private Customer Details must not be provided;");
                correct = false;
            }
        } else {
            if(request.getBusinessActivity() == null){
                stringBuilder.append("businessActivity-Business Activity is required;");
                correct = false;
            }
            if (request.getPrivateCustomerDetails() == null) {
                stringBuilder.append("privateCustomerDetails-Private Customer Details is required;");
                correct = false;
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
