package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.SummaryDataRowParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = SummaryDataRowVatRateValidator.SummaryDataRowVatRateValidatorImpl.class)
public @interface SummaryDataRowVatRateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SummaryDataRowVatRateValidatorImpl implements ConstraintValidator<SummaryDataRowVatRateValidator, SummaryDataRowParameters> {
        @Override
        public boolean isValid(SummaryDataRowParameters rowParameters, ConstraintValidatorContext context) {

            if (rowParameters.getGlobalVatRate() != null && rowParameters.getGlobalVatRate()) {
                if (rowParameters.getVatRateId() != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is selected vat rate id should be null;").addConstraintViolation();
                    return false;
                }

            } else if(rowParameters.getGlobalVatRate() != null) {
                if (rowParameters.getVatRateId() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is not  selected vat rate id should not be null;").addConstraintViolation();
                    return false;
                }
            }

            return true;
        }
    }
}
