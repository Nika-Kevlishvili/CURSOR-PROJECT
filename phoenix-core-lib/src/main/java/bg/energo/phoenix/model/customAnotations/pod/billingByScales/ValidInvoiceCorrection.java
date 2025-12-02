package bg.energo.phoenix.model.customAnotations.pod.billingByScales;

import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScalesCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidInvoiceCorrection.InvoiceCorrectionValidator.class})
public @interface ValidInvoiceCorrection {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoiceCorrectionValidator implements ConstraintValidator<ValidInvoiceCorrection, BillingByScalesCreateRequest> {

        @Override
        public boolean isValid(BillingByScalesCreateRequest request, ConstraintValidatorContext context) {
            Boolean correction = request.getCorrection();
            Boolean override = request.getOverride();
            String invoiceCorrection = request.getInvoiceCorrection();

            if (correction == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("correction-[Correction] should be %s or %s;"
                        .formatted(true, false)).addConstraintViolation();
                return false;
            }

            if(invoiceCorrection != null){
                if(StringUtils.isBlank(invoiceCorrection)){
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("invoiceCorrection-[InvoiceCorrection] shouldn't be blank;").addConstraintViolation();
                    return false;
                }
            }
            if (correction) {
                if (StringUtils.isBlank(invoiceCorrection)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("invoiceCorrection-[InvoiceCorrection] when correction is true, InvoiceCorrection should not be empty;").addConstraintViolation();
                    return false;
                }
            }

            if (override) {
                if (!correction) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("override-[Override] shouldn't be checked if correction is not checked;").addConstraintViolation();
                    return false;
                }
            }

            return true;
        }

    }
}
