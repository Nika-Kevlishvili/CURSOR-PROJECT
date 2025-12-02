package phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerEditValidators;

import org.apache.commons.collections4.CollectionUtils;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.EditCustomerRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EditCustomerManagerValidatorImpl
        implements ConstraintValidator<EditCustomerManagerValidator, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest request, ConstraintValidatorContext context) {
        CustomerType customerType = request.getCustomerType();
        if (customerType == null) {
            return false;
        }
        if ((customerType == CustomerType.LEGAL_ENTITY ||
                customerType == CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY)
                && request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                && (CollectionUtils.isEmpty(request.getManagers()))) {

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Minimum one manager is required; ")
                    .addConstraintViolation();
            return false;
        }

        if (customerType == CustomerType.PRIVATE_CUSTOMER && !CollectionUtils.isEmpty(request.getManagers())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Cannot add manager to private customer; ").addConstraintViolation();
            return false;
        }

        return true;
    }
}
