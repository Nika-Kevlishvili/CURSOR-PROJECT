package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.InvoiceCorrectionParameters;
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
@Constraint(validatedBy = InvoiceCorrectionParametersValidator.InvoiceCorrectionParametersImpl.class)
@Deprecated
//Todo as of 2025/feb/06 this is no longer needed. If nothing changes this could be removed.
public @interface InvoiceCorrectionParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoiceCorrectionParametersImpl implements ConstraintValidator<InvoiceCorrectionParametersValidator, InvoiceCorrectionParameters> {
        @Override
        public boolean isValid(InvoiceCorrectionParameters request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();
            String listOfInvoices = request.getListOfInvoices();
            Long fileId = request.getFileId();
            if (listOfInvoices == null && fileId == null) {
                validationMessageBuilder.append("invoiceCorrectionParameters.listOfInvoices-Either a list of invoices or a file must be selected;");
            }
            if (!validationMessageBuilder.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }

    }
}
