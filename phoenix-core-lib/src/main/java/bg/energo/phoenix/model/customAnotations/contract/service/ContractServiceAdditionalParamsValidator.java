package bg.energo.phoenix.model.customAnotations.contract.service;

import bg.energo.phoenix.model.request.contract.service.ContractServiceAdditionalParamsRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ContractServiceAdditionalParamsValidator.ContractServiceAdditionalParamsValidatorImpl.class})
public @interface ContractServiceAdditionalParamsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContractServiceAdditionalParamsValidatorImpl implements ConstraintValidator<ContractServiceAdditionalParamsValidator, ServiceContractServiceParametersCreateRequest> {
        @Override
        public boolean isValid(ServiceContractServiceParametersCreateRequest request, ConstraintValidatorContext context) {
            List<ContractServiceAdditionalParamsRequest> productAdditionalParams = request.getContractServiceAdditionalParamsRequests();
            if (productAdditionalParams != null) {
                Set<Long> ids = new HashSet<>();
                for (ContractServiceAdditionalParamsRequest params : productAdditionalParams) {
                    if (params.getId() != null && !ids.add(params.getId())) {
                        context.buildConstraintViolationWithTemplate("serviceParameters.serviceAdditionalParams-Id must be unique among additional params;").addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}

