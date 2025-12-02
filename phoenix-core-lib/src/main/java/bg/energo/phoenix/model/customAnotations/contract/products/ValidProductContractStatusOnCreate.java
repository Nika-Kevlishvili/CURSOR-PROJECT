package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
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
@Constraint(validatedBy = {ValidProductContractStatusOnCreate.ProductContractStatusValidatorImpl.class})
public @interface ValidProductContractStatusOnCreate {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractStatusValidatorImpl implements ConstraintValidator<ValidProductContractStatusOnCreate, ProductContractBasicParametersCreateRequest> {
        @Override
        public boolean isValid(ProductContractBasicParametersCreateRequest value, ConstraintValidatorContext context) {
            if (value.getVersionStatus() != ProductContractVersionStatus.SIGNED) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "basicParameters.versionStatus-First version must be valid/signed!;"
                ).addConstraintViolation();
                return false;
            }
            ContractDetailsStatus status = value.getStatus();
            ContractDetailsSubStatus subStatus = value.getSubStatus();
            if (!status.equals(ContractDetailsStatus.DRAFT) && !status.equals(ContractDetailsStatus.READY)
                    && !status.equals(ContractDetailsStatus.SIGNED) && !status.equals(ContractDetailsStatus.ENTERED_INTO_FORCE)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("basicParameters.status-Status is invalid!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
