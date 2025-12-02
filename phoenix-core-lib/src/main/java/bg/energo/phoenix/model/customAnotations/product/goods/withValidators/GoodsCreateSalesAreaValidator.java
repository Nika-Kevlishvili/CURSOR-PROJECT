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
@Constraint(validatedBy = {GoodsCreateSalesAreaValidator.GoodsCreateSalesAreaValidatorImpl.class})
public @interface GoodsCreateSalesAreaValidator {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsCreateSalesAreaValidatorImpl implements ConstraintValidator<GoodsCreateSalesAreaValidator, CreateGoodsRequest> {
        @Override
        public boolean isValid(CreateGoodsRequest request, ConstraintValidatorContext context) {
            if (request.getGlobalSalesArea() != null) {
                if (!request.getGlobalSalesArea()) {
                    if (request.getSalesAreasIds() == null || request.getSalesAreasIds().size() < 1) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("salesAreasIds-At least one sales area is required;")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (request.getSalesAreasIds() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("salesAreasIds-Sales area IDs must not be provided;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
