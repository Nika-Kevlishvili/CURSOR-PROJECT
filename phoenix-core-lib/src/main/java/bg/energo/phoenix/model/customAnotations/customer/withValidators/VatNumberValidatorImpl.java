package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
        StringBuilder stringBuilder = new StringBuilder();
        if (request.getCustomerType() == null) {
            stringBuilder.append("customerType-Unknown customer Type;");
            return false;
        }
        context.disableDefaultConstraintViolation();
        switch (request.getCustomerType()) {
            case LEGAL_ENTITY -> {
                if (request.getVatNumber() != null) {
                    if (request.getForeign()) {
                        Pattern pattern = Pattern.compile("^[A-Z–\\-./\\d]{5,15}$");
                        Matcher matcher = pattern.matcher(request.getVatNumber());
                        if (!matcher.matches()) {
                            stringBuilder.append("vatNumber-VAT number invalid format or symbols;");
                        }
                    } else {
                        Pattern pattern = Pattern.compile("^(BG\\d{9}|BG\\d{13})$");
                        Matcher matcher = pattern.matcher(request.getVatNumber());
                        if (!matcher.matches()) {
                            stringBuilder.append("vatNumber-VAT number invalid format or symbols;");
                        }
                    }
                }
            }
            case PRIVATE_CUSTOMER -> {
                if (request.getBusinessActivity() != null && request.getBusinessActivity()) {
                    if (request.getVatNumber() != null) {
                        if (request.getForeign()) {
                            Pattern pattern = Pattern.compile("^[A-Z–\\-.\\d]{5,15}$");
                            Matcher matcher = pattern.matcher(request.getVatNumber());
                            if (!matcher.matches()) {
                                stringBuilder.append("vatNumber-VAT number invalid format or symbols;");
                            }
                        } else {
                            Pattern pattern = Pattern.compile("^(BG\\d{10})$");
                            Matcher matcher = pattern.matcher(request.getVatNumber());
                            if (!matcher.matches()) {
                                stringBuilder.append("vatNumber-VAT number invalid format or symbols;");
                            }
                        }
                    }
                }
            }
        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return stringBuilder.isEmpty();
    }
}
