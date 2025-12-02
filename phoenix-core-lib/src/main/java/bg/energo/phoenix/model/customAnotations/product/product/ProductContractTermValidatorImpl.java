package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import bg.energo.phoenix.model.request.product.product.BaseProductTermsRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductContractTermValidatorImpl implements ConstraintValidator<ProductContractTermValidator, BaseProductTermsRequest> {
    @Override
    public boolean isValid(BaseProductTermsRequest request, ConstraintValidatorContext context) {
        boolean isValid = true;
        StringBuilder validationViolations = new StringBuilder();

        ProductTermPeriodType typeOfTerms = request.getTypeOfTerms();
        if (typeOfTerms == null) {
            return false;
        }

        Integer value = request.getValue();

        ProductTermType periodType = request.getPeriodType();
        if (typeOfTerms == ProductTermPeriodType.PERIOD) {
            if (periodType == null) {
                validationViolations.append("basicSettings.productTerms.periodType-Period type and value should be present when period type is PERIOD;");
                isValid = false;
            }

            if (value == null) {
                validationViolations.append("basicSettings.productTerms.value-Period type and value should be present when period type is PERIOD;");
                isValid = false;
            }

            if (Boolean.FALSE.equals(request.getAutomaticRenewal()) && request.getNumberOfRenewals() != null) {
                validationViolations.append("basicSettings.productTerms.numberOfRenewals-Number of renewals should not be present when automatic renewal is false;");
                isValid = false;
            }

            if (Boolean.TRUE.equals(request.getAutomaticRenewal())) {
                if (request.getRenewalPeriodType() == null) {
                    validationViolations.append("basicSettings.productTerms-renewal period type must be presented when automatic renewal is true;");
                    isValid = false;
                }

                if (request.getRenewalPeriodValue() == null) {
                    validationViolations.append("basicSettings.productTerms-renewal period value must be presented when automatic renewal is true;");
                    isValid = false;
                }
            }
        } else {
            if (periodType != null) {
                validationViolations.append("basicSettings.productTerms.periodType-Period type and value should not be present when period type is not PERIOD;");
                isValid = false;
            }

            if (value != null) {
                validationViolations.append("basicSettings.productTerms.value-Period type and value should not be present when period type is not PERIOD;");
                isValid = false;
            }

            if (request.getAutomaticRenewal() != null || request.getNumberOfRenewals() != null) {
                validationViolations.append("basicSettings.productTerms.automaticRenewal-Automatic renewal information should not be present when period type is not PERIOD;");
                isValid = false;
            }
        }

        if (!isValid) {
            context.buildConstraintViolationWithTemplate(validationViolations.toString()).addConstraintViolation();
        }
        return isValid;
    }
}
