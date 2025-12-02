package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.DetailedDataRowParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = DetailedDataRowDateRangeValidator.DetailedDataRowDateRangeValidatorImpl.class)
public @interface DetailedDataRowDateRangeValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DetailedDataRowDateRangeValidatorImpl implements ConstraintValidator<DetailedDataRowDateRangeValidator, DetailedDataRowParameters> {
        @Override
        public boolean isValid(DetailedDataRowParameters detailedDataRowParameters, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder errorMessages = new StringBuilder();
            if (detailedDataRowParameters.getPeriodFrom() != null && detailedDataRowParameters.getPeriodTo() != null &&
                    !(detailedDataRowParameters.getPeriodFrom().isBefore(detailedDataRowParameters.getPeriodTo()) ||
                            detailedDataRowParameters.getPeriodFrom().equals(detailedDataRowParameters.getPeriodTo()))) {
                errorMessages.append("detailedDataRowParameters.periodFrom- [periodFrom] should be before or equal to [periodTo];");
                isValid = false;
            }
            if (detailedDataRowParameters.getGlobalVatRate() != null && detailedDataRowParameters.getGlobalVatRate()) {
                if (detailedDataRowParameters.getVatRateId() != null) {
                    errorMessages.append("vatRateId-When global vat rate is selected vat rate id should be null;");
                    isValid = false;
                }

            } else if (detailedDataRowParameters.getGlobalVatRate() != null) {
                if (detailedDataRowParameters.getVatRateId() == null) {
                    errorMessages.append("vatRateId-When global vat rate is not  selected vat rate id should not be null;");
                    isValid = false;
                }
            }
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMessages.toString()).addConstraintViolation();
            }
            return isValid;
        }

    }
}
