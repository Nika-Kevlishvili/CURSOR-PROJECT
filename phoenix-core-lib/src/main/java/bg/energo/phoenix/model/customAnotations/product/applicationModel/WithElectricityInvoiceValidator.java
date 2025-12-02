package bg.energo.phoenix.model.customAnotations.product.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityPeriodType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimeWithElectricityInvoiceRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Objects;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {WithElectricityInvoiceValidator.PeriodicityValidatorImpl.class})
public @interface WithElectricityInvoiceValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PeriodicityValidatorImpl implements ConstraintValidator<WithElectricityInvoiceValidator, OverTimeWithElectricityInvoiceRequest> {

        @Override
        public boolean isValid(OverTimeWithElectricityInvoiceRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            Boolean withEveryInvoice = Objects.requireNonNullElse(request.getWithEveryInvoice(), false);
            Boolean atMostOncePer = Objects.requireNonNullElse(request.getAtMostOncePer(), false);
            OverTimeWithElectricityPeriodType overTimeWithElectricityPeriodType = request.getOverTimeWithElectricityPeriodType();
            if (withEveryInvoice && atMostOncePer) {
                context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimeWithElectricityInvoiceRequest.withEveryInvoice-Only one of with every invoice and at most once per can be selected;").addConstraintViolation();
                return false;
            }
            if (withEveryInvoice && overTimeWithElectricityPeriodType != null) {
                context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimeWithElectricityInvoiceRequest.withEveryInvoice-At most once per Period must be null when with every invoice is selected;").addConstraintViolation();
                return false;
            }
            if (atMostOncePer && overTimeWithElectricityPeriodType == null) {
                context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimeWithElectricityInvoiceRequest.atMostOncePer-At most once per Period must not be null when at most once per is selected;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}
