package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.DetailedDataRowEditParameters;
import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.ManualInvoiceDetailedDataEditParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = DetailedDataRowIdsValidator.DetailedDataRowIdsValidatorImpl.class)
public @interface DetailedDataRowIdsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DetailedDataRowIdsValidatorImpl implements ConstraintValidator<DetailedDataRowIdsValidator, ManualInvoiceDetailedDataEditParameters> {
        @Override
        public boolean isValid(ManualInvoiceDetailedDataEditParameters detailedDataEditParameters, ConstraintValidatorContext context) {
            List<DetailedDataRowEditParameters> detailedDataRowList = detailedDataEditParameters.getDetailedDataRowParametersList();
            if (detailedDataRowList != null) {
                Set<Long> ids = new HashSet<>();
                for (DetailedDataRowEditParameters row : detailedDataRowList) {
                    if (row.getId() != null && !ids.add(row.getId())) {
                        context.buildConstraintViolationWithTemplate("ManualInvoiceDetailedDataEditParameters.detailedDataRowParametersList.Ids must be unique among detailed data rows;").addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
