package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.request.billing.billingRun.create.InvoiceReversalParameters;
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
@Constraint(validatedBy = InvoiceReversalParametersValidator.InvoiceReversalParametersImpl.class)
public @interface InvoiceReversalParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoiceReversalParametersImpl implements ConstraintValidator<InvoiceReversalParametersValidator, InvoiceReversalParameters> {
        @Override
        public boolean isValid(InvoiceReversalParameters request, ConstraintValidatorContext context) {
            if (request.getFileId() == null && request.getListOfInvoices() == null) {
                context.buildConstraintViolationWithTemplate("invoiceReversalParameters.listOfInvoices-Either a list of invoices or a file must be selected;")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
