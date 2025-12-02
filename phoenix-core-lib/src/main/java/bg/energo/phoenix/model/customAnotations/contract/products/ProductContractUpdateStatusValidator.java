package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersUpdateRequest;
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
@Constraint(validatedBy = {ProductContractUpdateStatusValidator.ProductContractStatusValidatorImpl.class})
public @interface ProductContractUpdateStatusValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractStatusValidatorImpl implements ConstraintValidator<ProductContractUpdateStatusValidator, ProductContractUpdateRequest> {
        @Override
        public boolean isValid(ProductContractUpdateRequest value, ConstraintValidatorContext context) {
            ProductContractBasicParametersUpdateRequest basicParameters = value.getBasicParameters();
            StringBuilder sb = new StringBuilder();
            if (basicParameters == null) {
                return true;
            }
            if (basicParameters.getVersionStatus() == null || basicParameters.getStatus() == null || basicParameters.getSubStatus() == null) {
                return true;
            }

            if (!sb.isEmpty()) {
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }

    }
}
