package bg.energo.phoenix.model.customAnotations.billing;

import bg.energo.phoenix.model.request.billing.invoice.CreateInvoiceCancellationRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InvoiceCancellationValidator.InvoiceCancellationCreateValidator.class})
public @interface InvoiceCancellationValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoiceCancellationCreateValidator implements ConstraintValidator<InvoiceCancellationValidator, CreateInvoiceCancellationRequest> {

        @Override
        public boolean isValid(CreateInvoiceCancellationRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessageBuilder = new StringBuilder();

            if (request.getInvoices() == null && request.getFileId() == null) {
                validationMessageBuilder.append("invoices-[invoices] or [fileId] must be defined;");
            }

            String invoice = request.getInvoices();
            if (StringUtils.isNotBlank(invoice)) {
                if (invoice.contains(",")) {
                    boolean isValidInvoices = Arrays
                            .stream(invoice.split(","))
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .anyMatch(s -> s.length() != 10);

                    if (isValidInvoices) {
                        validationMessageBuilder.append("invoices-each invoice number size should be 10 symbols");
                    }
                } else {
                    if (invoice.trim().length() != 10) {
                        validationMessageBuilder.append("invoices-each invoice number size should be 10 symbols");
                    }
                }
            }

            boolean isValid = validationMessageBuilder.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }

}
