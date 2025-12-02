package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BillingRunTemplateValidator.BillingRunTemplateValidatorImpl.class)
public @interface BillingRunTemplateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunTemplateValidatorImpl implements ConstraintValidator<BillingRunTemplateValidator, BillingRunCreateRequest> {
        @Override
        public boolean isValid(BillingRunCreateRequest createRequest, ConstraintValidatorContext context) {
            if (createRequest.getCommonParameters() == null) {
                return true;

            }
            BillingType billingType = createRequest.getBillingType();
            if (billingType == null) {
                return true;
            }
            Long templateId = createRequest.getCommonParameters().getTemplateId();
            Long emailTemplateId = createRequest.getCommonParameters().getEmailTemplateId();
            if (List.of(BillingType.MANUAL_INVOICE, BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT).contains(billingType) && templateId == null) {
                context.buildConstraintViolationWithTemplate("commonParameters.templateId-[templateId] template id can not be null!;").addConstraintViolation();
                return false;
            }
            if (List.of(BillingType.MANUAL_INVOICE, BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT).contains(billingType) && emailTemplateId == null) {
                context.buildConstraintViolationWithTemplate("commonParameters.emailTemplateId-[emailTemplateId] template id can not be null!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}
