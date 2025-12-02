package bg.energo.phoenix.model.customAnotations.product.goods.withValidators;

import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GoodsEditVatRateValidator.GoodsEditVatRateValidatorImpl.class})
public @interface GoodsEditVatRateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsEditVatRateValidatorImpl implements ConstraintValidator<GoodsEditVatRateValidator, EditGoodsRequest> {
        @Override
        public boolean isValid(EditGoodsRequest request, ConstraintValidatorContext context) {
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
