package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.CreateCustomerRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Check VAT Number format based on customer Type
    If LEGAL_ENTITY length must be 11 or 15
    If PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY length must be 12 or 14
 */
public class VatNumberValidatorImpl
        implements ConstraintValidator<VatNumberValidator, CreateCustomerRequest> {
    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        if (request.getCustomerType() == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        context.disableDefaultConstraintViolation();
        if(request.getCustomerType() == CustomerType.LEGAL_ENTITY){
            if(request.getVatNumber() != null){
                Pattern pattern = Pattern.compile("^(BG\\d{9}|BG\\d{13})$");
                Matcher matcher = pattern.matcher(request.getVatNumber());
                if(!matcher.matches()){
                    stringBuilder.append("VAT number invalid format or symbols; ");
                }
            }
        } else if(request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY) {
            if(request.getVatNumber() != null){
                Pattern pattern = Pattern.compile("^(BG\\d{10}|BG\\d{12})$");
                Matcher matcher = pattern.matcher(request.getVatNumber());
                if(!matcher.matches()){
                    stringBuilder.append("VAT number invalid format or symbols; ");
                }
            }
        }
        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return stringBuilder.isEmpty();
    }
}
