package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.request.CustomerAddressRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/*
    Validate address data based on customer is Foreign or not
 */
public class CustomerAddressValidatorImpl
        implements ConstraintValidator<CustomerAddressValidator, CustomerAddressRequest> {
    @Override
    public boolean isValid(CustomerAddressRequest customerAddressRequest,
                           ConstraintValidatorContext context) {
        if(customerAddressRequest.getForeign() == null){
            return false;
        }

        context.disableDefaultConstraintViolation();
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        if(customerAddressRequest.getForeign()){
            if(customerAddressRequest.getForeignAddressData() == null){
                stringBuilder.append("Foreign address data is required; ");
                correct = false;
            }
            if(customerAddressRequest.getLocalAddressData() != null){
                stringBuilder.append("Local address data must not be provided; ");
                correct = false;
            }
        }else{
            if(customerAddressRequest.getForeignAddressData() != null){
                stringBuilder.append("Foreign address data  must not be provided; ");
                correct = false;
            }
            if(customerAddressRequest.getLocalAddressData() == null){
                stringBuilder.append("Local address data is required; ");
                correct = false;
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
