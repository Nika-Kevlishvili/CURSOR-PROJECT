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
@Constraint(validatedBy = {ProductContractTerminationDateValidator.ProductContractTerminationDateValidatorImpl.class})
public @interface ProductContractTerminationDateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractTerminationDateValidatorImpl implements ConstraintValidator<ProductContractTerminationDateValidator, ProductContractBasicParametersUpdateRequest>{

        @Override
        public boolean isValid(ProductContractBasicParametersUpdateRequest productContractBasicParametersUpdateRequest, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            if (productContractBasicParametersUpdateRequest.getTerminationDate() != null && productContractBasicParametersUpdateRequest.getTerminationDate().isAfter(LocalDate.now())){
                context.buildConstraintViolationWithTemplate("basicParameters.terminationDate-Termination date should be less or equal than current day;").addConstraintViolation();
                return false;
            }
            if (productContractBasicParametersUpdateRequest.getTerminationDate() != null && (productContractBasicParametersUpdateRequest.getTerminationDate().isBefore(LocalDate.now())
                    ||productContractBasicParametersUpdateRequest.getTerminationDate().isEqual(LocalDate.now()))){
                if (!(productContractBasicParametersUpdateRequest.getStatus() == ContractDetailsStatus.TERMINATED)){
                    context.buildConstraintViolationWithTemplate("basicParameters.status-Contract Status should be Terminated;").addConstraintViolation();
                    return false;
                }
            }
            if (productContractBasicParametersUpdateRequest.getTerminationDate() == null && productContractBasicParametersUpdateRequest.getStatus() == ContractDetailsStatus.TERMINATED){
                context.buildConstraintViolationWithTemplate("basicParameters.terminationDate-Termination date should not be empty;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
