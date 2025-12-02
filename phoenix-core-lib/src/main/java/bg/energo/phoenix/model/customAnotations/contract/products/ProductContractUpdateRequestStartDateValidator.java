package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductContractUpdateRequestStartDateValidator.ProductContractUpdateRequestStartDateValidatorImpl.class})
public @interface ProductContractUpdateRequestStartDateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractUpdateRequestStartDateValidatorImpl implements ConstraintValidator<ProductContractUpdateRequestStartDateValidator, ProductContractUpdateRequest> {
        @Override
        public boolean isValid(ProductContractUpdateRequest value, ConstraintValidatorContext context) {
            if (value.isSavingAsNewVersion() && value.getStartDate() == null) {
                context.buildConstraintViolationWithTemplate("startDate-Start date can not be null!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
