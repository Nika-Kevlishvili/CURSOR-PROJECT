package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressContractTypeValidation.ExpressContractTypeValidationImpl.class})
public @interface ExpressContractTypeValidation {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExpressContractTypeValidationImpl implements ConstraintValidator<ExpressContractTypeValidation, ExpressContractRequest> {
        @Override
        public boolean isValid(ExpressContractRequest request, ConstraintValidatorContext context) {
            ExpressContractType expressContractType = request.getExpressContractType();
            if (expressContractType == null) {
                return true;
            }
            if (expressContractType.equals(ExpressContractType.PRODUCT) && request.getProductParameters() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("productParameters-Product parameters are not provided!;").addConstraintViolation();
                return false;
            }
            if (expressContractType.equals(ExpressContractType.SERVICE) && request.getServiceParameters() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("serviceParameters-Service parameters are not provided!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
