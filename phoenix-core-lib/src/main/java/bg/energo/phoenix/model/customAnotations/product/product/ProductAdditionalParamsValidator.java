package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
import bg.energo.phoenix.model.request.product.product.ProductsAdditionalParamsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductAdditionalParamsValidator.ProductAdditionalParamsValidatorImpl.class})
public @interface ProductAdditionalParamsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductAdditionalParamsValidatorImpl implements ConstraintValidator<ProductAdditionalParamsValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            List<ProductsAdditionalParamsRequest> productAdditionalParams = request.getProductAdditionalParams();
            if (productAdditionalParams == null || productAdditionalParams.isEmpty()) {
                return true;
            }

            boolean isValid = true;
            StringBuilder exceptionMessageBuilder = new StringBuilder();

            for (int i = 0; i < productAdditionalParams.size(); i++) {
                ProductsAdditionalParamsRequest productAdditionalParam = productAdditionalParams.get(i);

                if (productAdditionalParam.orderingId() == null) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-missing ordering id;", i));
                }

                if (productAdditionalParam.label().isEmpty()) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-missing label;", i));
                } else {
                    if (productAdditionalParam.label().length() > 1024) {
                        exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-label value size is more than 1024 symbol;", i));
                    }
                }

                if (productAdditionalParam.value() != null && productAdditionalParam.value().length() > 1024) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-value value size is more than 1024 symbol;", i));
                }
            }

            Map<Long, List<Integer>> duplicateIndexesMap = new HashMap<>();

            IntStream.range(0, productAdditionalParams.size())
                    .forEach(index -> {
                        long orderingId = productAdditionalParams.get(index).orderingId();
                        duplicateIndexesMap.computeIfAbsent(orderingId, k -> new ArrayList<>()).add(index);
                    });

            duplicateIndexesMap.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .forEach(entry ->
                            exceptionMessageBuilder.append("additionalSettings.additionalParam-duplicate orderingId ")
                                    .append("found at indexes: [").append(entry.getValue())
                                    .append("];"));


            if (!exceptionMessageBuilder.isEmpty()) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(exceptionMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
