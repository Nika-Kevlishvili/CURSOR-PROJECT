package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
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
@Constraint(validatedBy = {SaleDateAvailabilityValidator.SaleDateAvailabilityValidatorImpl.class})
public @interface SaleDateAvailabilityValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SaleDateAvailabilityValidatorImpl implements ConstraintValidator<SaleDateAvailabilityValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();

            if (request.getAvailableFrom() != null && request.getAvailableTo() != null && !request.getAvailableFrom().isBefore(request.getAvailableTo())) {
                validationMessageBuilder.append("basicSettings.availableFrom-[Available From Date] must be before [Available To Date];");
            }

            if (!validationMessageBuilder.isEmpty()) {
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
            return isValid;
        }
    }
}
