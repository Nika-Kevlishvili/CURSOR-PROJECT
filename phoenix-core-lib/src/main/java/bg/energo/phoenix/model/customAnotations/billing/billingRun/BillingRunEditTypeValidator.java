package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.edit.BillingRunEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BillingRunEditTypeValidator.BillingRunEditTypeValidatorImpl.class)
public @interface BillingRunEditTypeValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunEditTypeValidatorImpl implements ConstraintValidator<BillingRunEditTypeValidator, BillingRunEditRequest> {

        @Override
        public boolean isValid(BillingRunEditRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            if (request.getCommonParameters() == null) {
                validationMessages.append("baseRequest.commonParameters-[commonParameters] is mandatory;");
            }
            if (request.getBillingType() != null) {
                switch (request.getBillingType()) {
                    case MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                        if (request.getInterimAndAdvancePaymentParameters() == null) {
                            validationMessages.append("baseRequest.interimAndAdvancePaymentParameters-[interimAndAdvancePaymentParameters] is mandatory;");
                        }
                    }
                    case STANDARD_BILLING -> {
                        if (request.getBasicParameters() == null) {
                            validationMessages.append("baseRequest.basicParameters-[basicParameters] is mandatory;");
                        }
                    }
                    case MANUAL_INVOICE -> {
                        if (request.getManualInvoiceParameters() == null) {
                            validationMessages.append("baseRequest.manualInvoiceParameters-[manualInvoiceParameters] is mandatory;");
                        }
                    }
                }
            }
            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }
    }
}
