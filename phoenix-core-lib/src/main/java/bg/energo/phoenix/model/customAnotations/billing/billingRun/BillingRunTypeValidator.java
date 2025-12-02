package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
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
@Constraint(validatedBy = BillingRunTypeValidator.BillingRunTypeValidatorImpl.class)
public @interface BillingRunTypeValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunTypeValidatorImpl implements ConstraintValidator<BillingRunTypeValidator, BillingRunCreateRequest> {

        @Override
        public boolean isValid(BillingRunCreateRequest request, ConstraintValidatorContext context) {
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
                    case MANUAL_CREDIT_OR_DEBIT_NOTE -> {
                        if (request.getManualCreditOrDebitNoteParameters() == null) {
                            validationMessages.append("baseRequest.manualCreditOrDebitNoteParameters-[manualCreditOrDebitNoteParameters] is mandatory;");
                        }
                    }
                    case INVOICE_REVERSAL -> {
                        if (request.getInvoiceReversalParameters() == null) {
                            validationMessages.append("baseRequest.invoiceReversalParameters-[invoiceReversalParameters] is mandatory;");
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
