package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.CreateCustomerRequest;
import phoenix.core.customer.util.IdentificationNumberChecker;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Validate customer identifier based on customer Type:
    if LEGAL_ENTITY length must be 9 or 13
    if PRIVATE length must be 10 (Local customer) or 12 (Foreign customer)
 */
public class CustomerIdentifierValidatorImpl
        implements ConstraintValidator<CustomerIdentifierValidator, CreateCustomerRequest> {

    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        StringBuilder stringBuilder = new StringBuilder();
        if (request.getCustomerType() == null || request.getForeign() == null
                || request.getCustomerIdentifier() == null) {
            return false;
        }else {

            ArrayList<Integer> numbers = new ArrayList<>();
            if (request.getCustomerIdentifier().length() != 12) {
                try {
                    numbers = IdentificationNumberChecker.getNumberArrayFromString(request.getCustomerIdentifier());
                } catch (Exception e) {
                    stringBuilder.append("Customer Identifier invalid format or symbols; ");
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(stringBuilder.toString()).
                            addConstraintViolation();
                    return false;
                }
            }
            context.disableDefaultConstraintViolation();
            if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
                if(numbers.size() == 9){
                    if(!IdentificationNumberChecker.checkNineDigitUICNumber(numbers)){
                        stringBuilder.append("Customer Identifier invalid format or symbols; ");
                    }
                }else if(numbers.size() == 13){
                    if(IdentificationNumberChecker.checkThirteenDigitUICNumber(numbers)){
                        stringBuilder.append("Customer Identifier invalid format or symbols; ");
                    }
                }else{
                    stringBuilder.append("Customer Identifier invalid format or symbols; ");
                }
            } else {
                if (request.getForeign()) {
                    Pattern pattern = Pattern.compile("^\\d{8}[\\dA-Z]{4}$");
                    Matcher matcher = pattern.matcher(request.getCustomerIdentifier());
                    if (!matcher.matches()){
                        stringBuilder.append("Customer Identifier invalid format or symbols; ");
                    }
                }else {
                    if (numbers.size() != 10 || !IdentificationNumberChecker.personalNumber(numbers)) {
                        stringBuilder.append("Customer Identifier invalid format or symbols; ");
                    }
                }
            }
        }
        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).
                addConstraintViolation();
        return stringBuilder.isEmpty();
    }

}
