package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductContractUntilValidator.ProductContractUntilValidatorImpl.class})
public @interface ProductContractUntilValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractUntilValidatorImpl implements ConstraintValidator<ProductContractUntilValidator, ProductContractBasicParametersCreateRequest> {
        @Override
        public boolean isValid(ProductContractBasicParametersCreateRequest value, ConstraintValidatorContext context) {
            if(value==null){
                return true;
            }
            boolean untilVolume = value.isHasUntilVolume();
            boolean untilAmount = value.isHasUntilAmount();
            if (untilAmount && untilVolume){
                context.buildConstraintViolationWithTemplate("basicParameters.isUntilVolume-both isUntilVolume and isUntilAmount can not be true;").addConstraintViolation();
                return false;
            }
            if(untilAmount && value.getUntilAmount()==null){
                context.buildConstraintViolationWithTemplate("basicParameters.untilAmount-untilAmount can not be null when isUntilAmount is checked;").addConstraintViolation();

                return false;
            }
            if(untilAmount && value.getUntilAmountCurrencyId()==null){
                context.buildConstraintViolationWithTemplate("basicParameters.untilAmountCurrencyId-untilAmountCurrencyId can not be null when isUntilAmount is checked;").addConstraintViolation();

                return false;
            }
            if(untilVolume&& value.getUntilVolume()==null){
                context.buildConstraintViolationWithTemplate("basicParameters.untilVolume-untilVolume can not be null when isUntilVolume is checked;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
