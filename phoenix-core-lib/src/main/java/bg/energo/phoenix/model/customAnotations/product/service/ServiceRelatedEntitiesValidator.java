package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.enums.product.product.EntityType;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import bg.energo.phoenix.model.request.product.service.ServiceRelatedEntityRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceRelatedEntitiesValidator.ServiceRelatedEntitiesValidatorImpl.class})
public @interface ServiceRelatedEntitiesValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceRelatedEntitiesValidatorImpl implements ConstraintValidator<ServiceRelatedEntitiesValidator, List<ServiceRelatedEntityRequest>> {
        @Override
        public boolean isValid(List<ServiceRelatedEntityRequest> relatedEntities, ConstraintValidatorContext context) {
            if (CollectionUtils.isEmpty(relatedEntities)) {
                return true;
            }

            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();

            Set<Long> duplicatedProductIdsHolder = new HashSet<>();
            Set<Long> duplicatedServiceIdsHolder = new HashSet<>();

            for (int i = 0; i < relatedEntities.size(); i++) {
                ServiceRelatedEntityRequest relatedEntity = relatedEntities.get(i);

                Long id = relatedEntity.getId();
                if (id == null) {
                    validationMessageBuilder.append("basicSettings.relatedEntities[%s].id-Related entity id must not be null;".formatted(i));
                    isValid = false;
                }

                EntityType type = relatedEntity.getType();
                if (type == null) {
                    validationMessageBuilder.append("basicSettings.relatedEntities[%s].type-Related entity type must not be null;".formatted(i));
                    isValid = false;
                }

                ServiceObligationCondition obligatory = relatedEntity.getObligatory();
                if (obligatory == null) {
                    validationMessageBuilder.append("basicSettings.relatedEntities[%s].obligatory-Related entity obligatory must not be null;".formatted(i));
                    isValid = false;
                }

                ServiceAllowsSalesUnder allowSalesUnder = relatedEntity.getAllowSalesUnder();
                if (allowSalesUnder == null) {
                    validationMessageBuilder.append("basicSettings.relatedEntities[%s].allowSalesUnder-Related entity allow sales under must not be null;".formatted(i));
                    isValid = false;
                }

                if (type != null && id != null) {
                    switch (type) {
                        case PRODUCT -> {
                            if (!duplicatedProductIdsHolder.add(id)) {
                                validationMessageBuilder.append("basicSettings.relatedEntities[%s].id-Duplicated id found for related product with id [%s];".formatted(i, id));
                                isValid = false;
                            }
                        }
                        case SERVICE -> {
                            if (!duplicatedServiceIdsHolder.add(id)) {
                                validationMessageBuilder.append("basicSettings.relatedEntities[%s].id-Duplicated id found for related service with id [%s];".formatted(i, id));
                                isValid = false;
                            }
                        }
                    }
                }
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
