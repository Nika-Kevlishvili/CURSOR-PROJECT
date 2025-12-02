package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/*
    When customer is creating only two statuses must be allowed to assign: POTENTIAL and NEW
 */
public class CustomerStatusWhileCreatingValidatorImpl
        implements ConstraintValidator<CustomerStatusWhileCreatingValidator, CustomerDetailStatus> {

    @Override
    public boolean isValid(CustomerDetailStatus customerDetailStatus, ConstraintValidatorContext context) {
        if(customerDetailStatus == null){
            return false;
        }
        if(customerDetailStatus != CustomerDetailStatus.POTENTIAL && customerDetailStatus != CustomerDetailStatus.NEW){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Can not assign customer status \""
                    + customerDetailStatus.name() + "\" while creating").addConstraintViolation();
            return false;
        }
        return true;
    }
}
