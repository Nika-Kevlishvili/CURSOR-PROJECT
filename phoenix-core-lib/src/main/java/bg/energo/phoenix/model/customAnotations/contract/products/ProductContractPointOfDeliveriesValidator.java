package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.ProductContractPointOfDeliveryRequest;
import bg.energo.phoenix.util.epb.EPBListUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductContractPointOfDeliveriesValidator.ProductContractPointOfDeliveriesValidatorImpl.class})
public @interface ProductContractPointOfDeliveriesValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractPointOfDeliveriesValidatorImpl implements ConstraintValidator<ProductContractPointOfDeliveriesValidator, List<ProductContractPointOfDeliveryRequest>> {
        @Override
        public boolean isValid(List<ProductContractPointOfDeliveryRequest> models, ConstraintValidatorContext context) {
            if (CollectionUtils.isNotEmpty(models)) {
                context.disableDefaultConstraintViolation();

                List<Long> pointOfDeliveryDetailIds =
                        models
                                .stream()
                                .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                                .filter(Objects::nonNull)
                                .toList();

                String errorMessage =
                        EPBListUtils
                                .validateDuplicateValuesByIndexes(
                                        pointOfDeliveryDetailIds,
                                        "podRequests.productContractPointOfDeliveries.pointOfDeliveryDetailId"
                                );

                if (StringUtils.isNotBlank(errorMessage)) {
                    context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                    return false;
                }
            }

            return true;
        }
    }
}
