package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
            context.buildConstraintViolationWithTemplate("customerDetailStatus-Can not assign customer status \""
                    + customerDetailStatus.name() + "\";").addConstraintViolation();
            return false;
        }
        return true;
    }
}
