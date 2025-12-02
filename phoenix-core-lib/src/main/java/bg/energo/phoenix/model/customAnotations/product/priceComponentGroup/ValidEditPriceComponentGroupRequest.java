package bg.energo.phoenix.model.customAnotations.product.priceComponentGroup;

import bg.energo.phoenix.model.request.product.price.priceComponentGroup.EditPriceComponentGroupRequest;
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
@Constraint(validatedBy = {ValidEditPriceComponentGroupRequest.EditPriceComponentGroupRequestValidator.class})
public @interface ValidEditPriceComponentGroupRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditPriceComponentGroupRequestValidator implements ConstraintValidator<ValidEditPriceComponentGroupRequest, EditPriceComponentGroupRequest> {

        @Override
        public boolean isValid(EditPriceComponentGroupRequest request, ConstraintValidatorContext context) {
            if (request.getUpdateExistingVersion() == null) {
                context.buildConstraintViolationWithTemplate("updateExistingVersion-[updateExistingVersion] field must not be null;").addConstraintViolation();
                return false;
            }

            if (request.getUpdateExistingVersion()) {
                if (request.getStartDate() != null) {
                    context.buildConstraintViolationWithTemplate("startDate-Start date cannot be changed when updating an existing version;").addConstraintViolation();
                    return false;
                }
            } else {
                if (request.getStartDate() == null) {
                    context.buildConstraintViolationWithTemplate("startDate-Start date must be provided when creating a new version;").addConstraintViolation();
                    return false;
                }
            }
            return true;
        }

    }
}