package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    Validate address data based on customer is Foreign or not
 */
public class CustomerAddressValidatorImpl
        implements ConstraintValidator<CustomerAddressValidator, CreateCustomerRequest> {
    @Override
    public boolean isValid(CreateCustomerRequest createCustomerRequest,
                           ConstraintValidatorContext context) {
        CustomerAddressRequest customerAddressRequest = createCustomerRequest.getAddress();
        if (customerAddressRequest == null){
            if (createCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
                return true;
            else
                return false;
        }

        if(customerAddressRequest.getForeign() == null){
            return false;
        }

        context.disableDefaultConstraintViolation();
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        if(customerAddressRequest.getForeign()){
            if(customerAddressRequest.getForeignAddressData() == null && !createCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL)){
                stringBuilder.append("address.foreignAddressData-Foreign address data is required;");
                correct = false;
            }
            if(customerAddressRequest.getLocalAddressData() != null){
                stringBuilder.append("address.localAddressData-Local address data must not be provided;");
                correct = false;
            }
        }else{
            if(customerAddressRequest.getForeignAddressData() != null){
                stringBuilder.append("address.foreignAddressData-Foreign address data  must not be provided;");
                correct = false;
            }
            if(customerAddressRequest.getLocalAddressData() == null && !createCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL)){
                stringBuilder.append("address.localAddressData-Local address data is required;");
                correct = false;
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
