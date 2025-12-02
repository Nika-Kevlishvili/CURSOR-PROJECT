package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressContractParametersValidator.ExpressContractParametersValidatorImpl.class})
public @interface ExpressContractParametersValidator {

    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExpressContractParametersValidatorImpl implements ConstraintValidator<ExpressContractParametersValidator, ExpressContractRequest> {
        @Override
        public boolean isValid(ExpressContractRequest request, ConstraintValidatorContext context) {
            ExpressContractType type = request.getExpressContractType();
            if(type==null){
                return true;
            }
            if(type.equals(ExpressContractType.PRODUCT)){
                ExpressContractParameters expressContractParameters = request.getExpressContractParameters();
                if(expressContractParameters ==null){
                    return true;
                }
                if(expressContractParameters.getEstimatedTotalConsumption()==null){
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("additionalParameters.estimatedTotalConsumptionUnderContractKwh-Field is mandatory;").addConstraintViolation();
                    return false;
                }
                if(CollectionUtils.isEmpty(request.getPodDetailIds())){
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("additionalParameters.podDetailIds-shouldn't be empty;").addConstraintViolation();
                    return false;
                }
            }
            return true;
        }

    }
}
