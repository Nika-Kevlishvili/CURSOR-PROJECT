package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.enums.product.product.ProductPodType;
import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {IndividualProductValidator.IndividualProductValidatorImpl.class})
public @interface IndividualProductValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IndividualProductValidatorImpl implements ConstraintValidator<IndividualProductValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            if (BooleanUtils.isTrue(request.getIsIndividual())) {
                if (StringUtils.isEmpty(request.getCustomerIdentifier())) {
                    violations.append("basicSettings.customerIdentifier-Customer identifier must not be blank for individual product;");
                }

                if (request.getAvailableForSale() != null) {
                    violations.append("basicSettings.availableForSale-Available For Sale must be null for individual product;");
                }

                if (request.getAvailableFrom() != null) {
                    violations.append("basicSettings.availableFrom-Available From must be null for individual product;");
                }

                if (request.getAvailableTo() != null) {
                    violations.append("basicSettings.availableTo-Available To must be null for individual product;");
                }

                if (request.getGlobalSalesChannel() != null || !CollectionUtils.isEmpty(request.getSalesChannelIds())) {
                    violations.append("basicSettings.salesChannelIds-Sales Channels is disabled for individual product;");
                }

                if (request.getGlobalSalesArea() != null || !CollectionUtils.isEmpty(request.getSalesAreasIds())) {
                    violations.append("basicSettings.salesAreasIds-Sales Area is disabled for individual product;");
                }

                if (request.getGlobalSegment() != null || !CollectionUtils.isEmpty(request.getSegmentIds())) {
                    violations.append("basicSettings.segmentIds-Segments is disabled for individual product;");
                }
            } else {
                if (StringUtils.isNotBlank(request.getCustomerIdentifier())) {
                    violations.append("basicSettings.customerIdentifier-Customer identifier must be blank for standard product;");
                }

                if (request.getProductGroupId() == null) {
                    violations.append("basicSettings.productGroupId-Product Group ID must not be null;");
                }

                if (request.getShortDescription() == null) {
                    violations.append("basicSettings.shortDescription-Short Description must not be blank;");
                }

                if (request.getAvailableForSale() == null) {
                    violations.append("basicSettings.availableForSale-Available For Sale must not be null;");
                }

                if (request.getGlobalSalesChannel() == null) {
                    violations.append("basicSettings.globalSalesChannel-Global Sales Channel must not be null;");
                }

                if (request.getGlobalSalesArea() == null) {
                    violations.append("basicSettings.globalSalesArea-Global Sales Area must not be null;");
                }

                if (request.getGlobalSegment() == null) {
                    violations.append("basicSettings.globalSegment-Must not be null;");
                }

                if (BooleanUtils.toBoolean(request.getGlobalVatRate())) {
                    if (request.getVatRateId() != null) {
                        violations.append("basicSettings.vatRateId-Vat Rate ID must be null while [globalVatRate] is true;");
                    }
                } else {
                    if (request.getVatRateId() == null) {
                        violations.append("basicSettings.vatRateId-Vat Rate ID must not be null while [globalVatRate] is false;");
                    }
                }

                if (BooleanUtils.toBoolean(request.getGlobalSalesArea())) {
                    if (request.getSalesAreasIds() != null) {
                        violations.append("basicSettings.salesAreasIds-Sales Areas IDs must be null while [globalSalesAreas] is true;");
                    }
                } else if (CollectionUtils.isEmpty(request.getSalesAreasIds())) {
                    violations.append("basicSettings.salesAreasIds-Sales Area IDs must contain at least one object while [globalSalesAreas] is false;");
                }

                if (BooleanUtils.toBoolean(request.getGlobalSalesChannel())) {
                    if (request.getSalesChannelIds() != null) {
                        violations.append("basicSettings.salesChannelIds-Sales Channel IDs must be null while [globalSalesChannel] is true;");
                    }
                } else if (CollectionUtils.isEmpty(request.getSalesChannelIds())) {
                    violations.append("basicSettings.salesChannelIds-Sales Channel IDs must contain at least one object while [globalSalesChannel] is false;");
                }

                if (BooleanUtils.toBoolean(request.getGlobalSegment())) {
                    if (request.getSegmentIds() != null) {
                        violations.append("basicSettings.segmentIds-Segment IDs must be null while [globalSegment] is true;");
                    }
                } else if (CollectionUtils.isEmpty(request.getSegmentIds())) {
                    violations.append("basicSettings.segmentIds-Segment IDs must contain at least one object while [globalSegment] is false;");
                }

                if (StringUtils.isBlank(request.getName())) {
                    violations.append("basicSettings.name-Name must not be blank;");
                }

                if (StringUtils.isBlank(request.getNameTransliterated())) {
                    violations.append("basicSettings.nameTransliterated-Transliterated Name must not be blank;");
                }
            }

            if (request.getTypePointsOfDelivery() != null && request.getTypePointsOfDelivery().size() == 1) {
                if (request.getConsumerBalancingProductNameId() != null && request.getTypePointsOfDelivery().contains(ProductPodType.GENERATOR)) {
                    violations.append("basicSettings.consumerBalancingProductNameId-ConsumerBalancingProductNameId must be null, because pod GENERATOR type is selected;");
                }

                if (request.getGeneratorBalancingProductNameId() != null && request.getTypePointsOfDelivery().contains(ProductPodType.CONSUMER)) {
                    violations.append("basicSettings.generatorBalancingProductNameId-GeneratorBalancingProductNameId must be null, because pod CONSUMER type is selected;");
                }
            }

            if (!violations.isEmpty()) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
