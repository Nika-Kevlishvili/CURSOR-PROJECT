package bg.energo.phoenix.model.customAnotations.pod.billingByScales;

import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScalesCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.Month;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidInvoiceDate.InvoiceDateValidator.class})
public @interface ValidInvoiceDate {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoiceDateValidator implements ConstraintValidator<ValidInvoiceDate, BillingByScalesCreateRequest> {

        private static final LocalDateTime MIN_DATE = LocalDateTime.of(1990, Month.JANUARY, 1, 0, 0);
        private static final LocalDateTime MAX_DATE = LocalDateTime.of(2090, Month.DECEMBER, 31, 23, 59, 59);

        @Override
        public boolean isValid(BillingByScalesCreateRequest request, ConstraintValidatorContext context) {
            LocalDateTime invoiceDate = request.getInvoiceDate();

            if (invoiceDate.isBefore(MIN_DATE) || invoiceDate.isAfter(MAX_DATE)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("invoiceDate-[InvoiceDate] must be between %s and %s;"
                        .formatted(MIN_DATE.toString(), MAX_DATE.toString())).addConstraintViolation();
                return false;
            }

            return true;
        }

    }
}
