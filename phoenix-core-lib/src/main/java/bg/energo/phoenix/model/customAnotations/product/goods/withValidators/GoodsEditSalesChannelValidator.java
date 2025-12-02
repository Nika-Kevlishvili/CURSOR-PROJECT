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
@Constraint(validatedBy = {GoodsEditSalesChannelValidator.GoodsEditSalesChannelValidatorImpl.class})
public @interface GoodsEditSalesChannelValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsEditSalesChannelValidatorImpl implements ConstraintValidator<GoodsEditSalesChannelValidator, EditGoodsRequest> {
        @Override
        public boolean isValid(EditGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSalesChannel() != null) {
                if (!request.getGlobalSalesChannel()) {
                    if (request.getSalesChannelsIds() == null || request.getSalesChannelsIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSalesChannels-At least one sales channel is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSalesChannelsIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSalesChannels-Sales channel IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }


}
