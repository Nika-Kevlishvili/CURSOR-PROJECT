package bg.energo.phoenix.model.customAnotations.product.goods.withValidators;

import bg.energo.phoenix.model.request.product.goods.CreateGoodsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GoodsCreateVatRateValidator.GoodsCreateVatRateValidatorImpl.class})
public @interface GoodsCreateVatRateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsCreateVatRateValidatorImpl implements ConstraintValidator<GoodsCreateVatRateValidator, CreateGoodsRequest> {
        @Override
        public boolean isValid(CreateGoodsRequest request, ConstraintValidatorContext context) {

            if (request.getGlobalVatRate() != null && request.getGlobalVatRate()) {
                if (request.getVatRateId() != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is selected vat rate id should be null;").addConstraintViolation();
                    return false;
                }

            } else {
                if (request.getVatRateId() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("vatRateId-When global vat rate is not  selected vat rate id should not be null;").addConstraintViolation();
                    return false;
                }
            }

            return true;
        }
    }
}
