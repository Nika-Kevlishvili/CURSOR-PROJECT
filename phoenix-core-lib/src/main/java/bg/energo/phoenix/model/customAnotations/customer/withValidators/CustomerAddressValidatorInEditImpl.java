package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CustomerAddressValidatorInEditImpl implements ConstraintValidator<CustomerAddressValidatorInEdit, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest editCustomerRequest,
                           ConstraintValidatorContext context) {
        CustomerAddressRequest customerAddressRequest = editCustomerRequest.getAddress();
        if (customerAddressRequest == null){
            if (editCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
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
            if(customerAddressRequest.getForeignAddressData() == null && !editCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL)){
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
            if(customerAddressRequest.getLocalAddressData() == null && !editCustomerRequest.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL)){
                stringBuilder.append("address.localAddressData-Local address data is required;");
                correct = false;
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
