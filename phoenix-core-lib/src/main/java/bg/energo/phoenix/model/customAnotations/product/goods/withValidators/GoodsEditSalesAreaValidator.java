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
@Constraint(validatedBy = {GoodsEditSalesAreaValidator.GoodsEditSalesAreaValidatorImpl.class})
public @interface GoodsEditSalesAreaValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsEditSalesAreaValidatorImpl implements ConstraintValidator<GoodsEditSalesAreaValidator, EditGoodsRequest> {
        @Override
        public boolean isValid(EditGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSalesArea() != null) {
                if (!request.getGlobalSalesArea()) {
                    if (request.getSalesAreasIds() == null || request.getSalesAreasIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSalesAreas-At least one sales area is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSalesAreasIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("goodsSalesAreas-Sales area IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
