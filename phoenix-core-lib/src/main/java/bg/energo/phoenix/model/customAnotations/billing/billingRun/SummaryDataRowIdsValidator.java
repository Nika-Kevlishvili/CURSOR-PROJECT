package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.ManualInvoiceSummaryDataEditParameters;
import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.SummaryDataRowEditParameters;
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
@Constraint(validatedBy = SummaryDataRowIdsValidator.SummaryDataRowIdsValidatorImpl.class)
public @interface SummaryDataRowIdsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SummaryDataRowIdsValidatorImpl implements ConstraintValidator<SummaryDataRowIdsValidator, ManualInvoiceSummaryDataEditParameters> {
        @Override
        public boolean isValid(ManualInvoiceSummaryDataEditParameters summaryDataEditParameters, ConstraintValidatorContext context) {
            List<SummaryDataRowEditParameters> summaryDataRowList = summaryDataEditParameters.getSummaryDataRowList();
            if (summaryDataRowList != null) {
                Set<Long> ids = new HashSet<>();
                for(SummaryDataRowEditParameters row : summaryDataRowList) {
                    if(row.getId() != null && !ids.add(row.getId())) {
                        context.buildConstraintViolationWithTemplate("manualInvoiceSummaryDataEditParameters.summaryDataRowList.Ids must be unique among summary data rows;").addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }

    }
}
