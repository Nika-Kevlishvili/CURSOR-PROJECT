package bg.energo.phoenix.model.customAnotations.product.priceComponent;

import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Objects;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PriceComponentVatRateValidator.PriceComponentVatRateValidatorImpl.class})
public @interface PriceComponentVatRateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PriceComponentVatRateValidatorImpl implements ConstraintValidator<PriceComponentVatRateValidator, PriceComponentRequest> {
        @Override
        public boolean isValid(PriceComponentRequest request, ConstraintValidatorContext context) {

            if (request.getGlobalVatRate() != null && request.getGlobalVatRate()) {
                if (request.getVatRateId() != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is selected vat rate id should be null;").addConstraintViolation();
                    return false;
                }

            } else {
                if (request.getVatRateId() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is not selected vat rate id should not be null;").addConstraintViolation();
                    return false;
                }
            }

            if (!Objects.requireNonNullElse(request.getDoNotIncludeVatBase(), false)) {
                if (request.getAlternativeRecipientCustomerDetailId() != null) {
                    context.buildConstraintViolationWithTemplate("alternateLiability-Alternate Liability must be null When Do not include in the VAT base is not selected;").addConstraintViolation();
                    return false;
                }
            }

            return true;
        }
    }
}
