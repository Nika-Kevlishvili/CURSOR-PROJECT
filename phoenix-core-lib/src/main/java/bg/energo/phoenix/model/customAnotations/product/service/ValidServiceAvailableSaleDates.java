package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.request.product.service.ServiceBasicSettingsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceAvailableSaleDates.ServiceAvailableSaleDatesValidator.class})
public @interface ValidServiceAvailableSaleDates {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceAvailableSaleDatesValidator implements ConstraintValidator<ValidServiceAvailableSaleDates, ServiceBasicSettingsRequest> {

        @Override
        public boolean isValid(ServiceBasicSettingsRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessageBuilder = new StringBuilder();

            Boolean availableForSale = request.getAvailableForSale();
            LocalDateTime availableFrom = request.getAvailableFrom();
            LocalDateTime availableTo = request.getAvailableTo();

            if (availableForSale == null) {
                // will be handled by @NotNull annotation
                return false;
            }

            if (availableForSale) {
                if (availableFrom != null && availableTo != null && !availableFrom.isBefore(availableTo)) {
                    validationMessageBuilder.append("basicSettings.availableFrom-[Available From Date] must be before [Available To Date];");
                }
            } else {
                if (availableFrom != null || availableTo != null) {
                    validationMessageBuilder.append("basicSettings.availableFrom-[Available From] and availableTo-[Available To] must be null while [Available For Sale] is false;");
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
