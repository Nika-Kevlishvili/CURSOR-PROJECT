package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.request.product.service.BaseServiceRequest;
import bg.energo.phoenix.model.request.product.service.ServiceAdditionalParamsRequest;
import bg.energo.phoenix.model.request.product.service.ServiceAdditionalSettingsRequest;
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
@Constraint(validatedBy = {ServiceAdditionalParamsValidator.ServiceAdditionalParamsValidatorImpl.class})
public @interface ServiceAdditionalParamsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceAdditionalParamsValidatorImpl implements ConstraintValidator<ServiceAdditionalParamsValidator, BaseServiceRequest> {
        @Override
        public boolean isValid(BaseServiceRequest request, ConstraintValidatorContext context) {
            ServiceAdditionalSettingsRequest additionalSettings = request.getAdditionalSettings();
            if (additionalSettings == null) {
                return true;
            }

            List<ServiceAdditionalParamsRequest> serviceAdditionalParams = additionalSettings.getServiceAdditionalParams();
            if (serviceAdditionalParams == null || serviceAdditionalParams.isEmpty()) {
                return true;
            }

            boolean isValid = true;
            StringBuilder exceptionMessageBuilder = new StringBuilder();

            for (int i = 0; i < serviceAdditionalParams.size(); i++) {
                ServiceAdditionalParamsRequest serviceAdditionalParam = serviceAdditionalParams.get(i);

                if (serviceAdditionalParam.orderingId() == null) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-missing ordering id;", i));
                }

                if (serviceAdditionalParam.label().isEmpty()) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-missing label;", i));
                } else {
                    if (serviceAdditionalParam.label().length() > 1024) {
                        exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-label value size is more than 1024 symbol;", i));
                    }
                }

                if (serviceAdditionalParam.value() != null && serviceAdditionalParam.value().length() > 1024) {
                    exceptionMessageBuilder.append(String.format("additionalSettings.additionalParam[%s]-value value size is more than 1024 symbol;", i));
                }
            }

            Map<Long, List<Integer>> duplicateIndexesMap = new HashMap<>();

            IntStream.range(0, serviceAdditionalParams.size())
                    .forEach(index -> {
                        long orderingId = serviceAdditionalParams.get(index).orderingId();
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
