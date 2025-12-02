package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersUpdateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductContractPerpetuityDateValidator.ProductContractPerpetuityDateValidatorImpl.class})
public @interface ProductContractPerpetuityDateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractPerpetuityDateValidatorImpl implements ConstraintValidator<ProductContractPerpetuityDateValidator, ProductContractBasicParametersUpdateRequest>{

        @Override
        public boolean isValid(ProductContractBasicParametersUpdateRequest productContractBasicParametersUpdateRequest, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            if (productContractBasicParametersUpdateRequest.getPerpetuityDate() != null && productContractBasicParametersUpdateRequest.getPerpetuityDate().isAfter(LocalDate.now())){
                context.buildConstraintViolationWithTemplate("basicParameters.perpetuityDate-Perpetuity date should be less or equal than current day;").addConstraintViolation();
                return false;
            }
            if (productContractBasicParametersUpdateRequest.getPerpetuityDate() != null && (productContractBasicParametersUpdateRequest.getPerpetuityDate().isBefore(LocalDate.now())
                    ||productContractBasicParametersUpdateRequest.getPerpetuityDate().isEqual(LocalDate.now()))){
                if (!productContractBasicParametersUpdateRequest.getStatus().equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY)){
                    context.buildConstraintViolationWithTemplate("basicParameters.status-Status should be Active in perpetuity;").addConstraintViolation();
                }
            }
            return true;
        }
    }
}
