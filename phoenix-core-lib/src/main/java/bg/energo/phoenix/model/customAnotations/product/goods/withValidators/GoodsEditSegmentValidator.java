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
@Constraint(validatedBy = {GoodsEditSegmentValidator.GoodsEditSegmentValidatorImpl.class})
public @interface GoodsEditSegmentValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsEditSegmentValidatorImpl implements ConstraintValidator<GoodsEditSegmentValidator, EditGoodsRequest> {
        @Override
        public boolean isValid(EditGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSegment() != null) {
                if (!request.getGlobalSegment()) {
                    if (request.getSegmentsIds() == null || request.getSegmentsIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSegments-At least one segment is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSegmentsIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSegments-Segment IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
