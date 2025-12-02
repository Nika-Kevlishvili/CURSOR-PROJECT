package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.CollectionUtils;

public class EditCustomerManagerValidatorImpl
        implements ConstraintValidator<EditCustomerManagerValidator, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest request, ConstraintValidatorContext context) {
        CustomerType customerType = request.getCustomerType();
        if (customerType == null) {
            return false;
        }

        if ((customerType == CustomerType.LEGAL_ENTITY)
                && request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                && (CollectionUtils.isEmpty(request.getManagers()))) {

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "managers-Minimum one manager is required;")
                    .addConstraintViolation();
            return false;
        }

        if (customerType.equals(CustomerType.PRIVATE_CUSTOMER)
                && (request.getBusinessActivity() != null && !request.getBusinessActivity())
                && !CollectionUtils.isEmpty(request.getManagers())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("managers-Cannot add manager to private customer;").addConstraintViolation();
            return false;
        }

        return true;
    }
}
