package phoenix.core.customer.apis.validation;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class ApisIdentificationNumberListValidatorImpl implements ConstraintValidator<ApisIdentificationNumberValidator, List<String>> {

    ApisIdentificationNumberValidator constraintAnnotation;

    @Override
    public void initialize(ApisIdentificationNumberValidator constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.constraintAnnotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        try {
            if(value == null){
                return false;
            }
            int arraySize = value.size();
            if (arraySize >= constraintAnnotation.minListSize() && arraySize <= constraintAnnotation.maxListSize()) {
                for (String item : value) {
                    if (!StringUtils.isNumeric(item)) {
                        addErrorMessage(item, context);
                        return false;
                    } else if (item.length() != constraintAnnotation.actualFirstLength()
                            && item.length() != constraintAnnotation.actualSecondLength()) {
                        addErrorMessage(item, context);
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addErrorMessage(String item, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Error in: " + item).addConstraintViolation();
    }
}
