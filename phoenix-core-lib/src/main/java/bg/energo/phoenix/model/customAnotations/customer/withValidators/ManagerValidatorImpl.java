package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.CollectionUtils;

/*
    When Customer Type is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
    minimum one manager is required
 */
public class ManagerValidatorImpl
        implements ConstraintValidator<ManagerValidator, CreateCustomerRequest> {
    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        CustomerType customerType = request.getCustomerType();
        if(customerType == null){
            return false;
        }

        if (customerType == CustomerType.LEGAL_ENTITY
                && request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                && (CollectionUtils.isEmpty(request.getManagers()))) {


            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "managers-Minimum one manager is required;")
                    .addConstraintViolation();
            return false;
        }

        if (customerType == CustomerType.PRIVATE_CUSTOMER
            && (request.getBusinessActivity() != null && !request.getBusinessActivity())
            && !CollectionUtils.isEmpty(request.getManagers())
        ) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("managers-Cannot add manager to private customer;")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
