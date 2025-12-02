package bg.energo.phoenix.model.customAnotations.product.priceComponent;

import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentFormulaRequest;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentFormulaVariableRequest;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {XenergieApplicationValidator.PriceComponentVatRateValidatorImpl.class})
public @interface XenergieApplicationValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PriceComponentVatRateValidatorImpl implements ConstraintValidator<XenergieApplicationValidator, PriceComponentRequest> {
        @Override
        public boolean isValid(PriceComponentRequest request, ConstraintValidatorContext context) {

            Boolean consumer = Objects.requireNonNullElse(request.getConsumer(), false);
            Boolean generator = Objects.requireNonNullElse(request.getGenerator(), false);
            PriceComponentFormulaRequest formulaRequest = request.getFormulaRequest();
            if (formulaRequest != null) {
                List<Long> productNameIds = Objects
                        .requireNonNullElse(formulaRequest.getVariables(), new ArrayList<PriceComponentFormulaVariableRequest>())
                        .stream()
                        .map(PriceComponentFormulaVariableRequest::getBalancingProfileNameId)
                        .filter(Objects::nonNull)
                        .toList();

                if (CollectionUtils.isNotEmpty(productNameIds) && !consumer && !generator) {
                    context.buildConstraintViolationWithTemplate("consumer-Xenergie application is mandatory if at least one Profile name is selected;").addConstraintViolation();
                    return false;
                }
            }

            if (consumer && generator) {
                context.buildConstraintViolationWithTemplate("consumer-only one of consumer and generator can be chosen;").addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
