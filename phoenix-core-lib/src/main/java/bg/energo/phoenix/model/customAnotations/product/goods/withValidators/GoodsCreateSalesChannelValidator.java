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
@Constraint(validatedBy = {GoodsCreateSalesChannelValidator.GoodsCreateSalesChannelValidatorImpl.class})
public @interface GoodsCreateSalesChannelValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsCreateSalesChannelValidatorImpl implements ConstraintValidator<GoodsCreateSalesChannelValidator, CreateGoodsRequest> {
        @Override
        public boolean isValid(CreateGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSalesChannel() != null) {
                if (!request.getGlobalSalesChannel()) {
                    if (request.getSalesChannelsIds() == null || request.getSalesChannelsIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("salesChannelsIds-At least one sales channel is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSalesChannelsIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("salesChannelsIds-Sales channel IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
