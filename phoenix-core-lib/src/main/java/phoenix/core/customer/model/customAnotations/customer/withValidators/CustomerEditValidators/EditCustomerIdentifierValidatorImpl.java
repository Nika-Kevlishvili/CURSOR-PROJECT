package phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerEditValidators;

import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.EditCustomerRequest;
import phoenix.core.customer.util.IdentificationNumberChecker;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditCustomerIdentifierValidatorImpl
        implements ConstraintValidator<EditCustomerIdentifierValidator, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest request, ConstraintValidatorContext context) {
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
