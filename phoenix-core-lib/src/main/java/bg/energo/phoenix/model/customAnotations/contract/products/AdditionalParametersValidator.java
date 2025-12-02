package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.ContractProductAdditionalParamsRequest;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
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
@Constraint(validatedBy = {AdditionalParametersValidator.AdditionalParametersValidatorImpl.class})
public @interface AdditionalParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AdditionalParametersValidatorImpl implements ConstraintValidator<AdditionalParametersValidator, ProductParameterBaseRequest> {
        @Override
        public boolean isValid(ProductParameterBaseRequest baseRequest, ConstraintValidatorContext context) {
            List<ContractProductAdditionalParamsRequest> productAdditionalParams = baseRequest.getProductAdditionalParams();
            if (productAdditionalParams != null) {
                Set<Long> ids = new HashSet<>();
                for (ContractProductAdditionalParamsRequest params : productAdditionalParams) {
                    if (params.getId() != null && !ids.add(params.getId())) {
                        context.buildConstraintViolationWithTemplate("productParameters.productAdditionalParams-Id must be unique among additional params;").addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
