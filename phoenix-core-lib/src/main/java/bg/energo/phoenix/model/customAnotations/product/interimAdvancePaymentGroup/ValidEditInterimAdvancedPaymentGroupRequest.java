package bg.energo.phoenix.model.customAnotations.product.interimAdvancePaymentGroup;

import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.AdvancedPaymentGroupEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidEditInterimAdvancedPaymentGroupRequest.EditInterimAdvancedPaymentGroupRequestValidator.class})
public @interface ValidEditInterimAdvancedPaymentGroupRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditInterimAdvancedPaymentGroupRequestValidator implements ConstraintValidator<ValidEditInterimAdvancedPaymentGroupRequest, AdvancedPaymentGroupEditRequest> {

        @Override
        public boolean isValid(AdvancedPaymentGroupEditRequest request, ConstraintValidatorContext context) {
            if (request.getUpdateExistingVersion() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("updateExistingVersion-[updateExistingVersion] field must not be null;").addConstraintViolation();
                return false;
            }

            if (request.getUpdateExistingVersion()) {
                if (request.getStartDate() != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("startDate-Start date cannot be changed when updating an existing version;").addConstraintViolation();
                    return false;
                }
            } else {
                if (request.getStartDate() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("startDate-Start date must be provided when creating a new version;").addConstraintViolation();
                    return false;
                }
            }

            return true;
        }

    }

}
