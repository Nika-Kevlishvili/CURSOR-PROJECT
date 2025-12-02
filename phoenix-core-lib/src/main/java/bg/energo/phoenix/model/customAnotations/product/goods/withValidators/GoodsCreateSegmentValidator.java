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
@Constraint(validatedBy = {GoodsCreateSegmentValidator.GoodsCreateSegmentValidatorImpl.class})
public @interface GoodsCreateSegmentValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsCreateSegmentValidatorImpl implements ConstraintValidator<GoodsCreateSegmentValidator, CreateGoodsRequest> {
        @Override
        public boolean isValid(CreateGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSegment() != null) {
                if (!request.getGlobalSegment()) {
                    if (request.getSegmentsIds() == null || request.getSegmentsIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("segmentsIds-At least one segment is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSegmentsIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("segmentsIds-Segment IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
