package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.request.product.service.ServiceBasicSettingsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidIndividualService.IndividualServiceValidator.class})
public @interface ValidIndividualService {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IndividualServiceValidator implements ConstraintValidator<ValidIndividualService, ServiceBasicSettingsRequest> {

        @Override
        public boolean isValid(ServiceBasicSettingsRequest basicSettings, ConstraintValidatorContext context) {
            StringBuilder errorMessage = new StringBuilder();

            if (BooleanUtils.isTrue(basicSettings.getIsIndividual())) {
                if (StringUtils.isEmpty(basicSettings.getCustomerIdentifier())) {
                    errorMessage.append("basicSettings.customerIdentifier-Customer identifier must not be blank for individual service;");
                }

                if (BooleanUtils.isNotFalse(basicSettings.getAvailableForSale())) {
                    errorMessage.append("basicSettings.availableForSale-[Available For Sale] must not be true for individual service;");
                }

                if (basicSettings.getAvailableFrom() != null) {
                    errorMessage.append("basicSettings.availableFrom-[Available From] must be null for individual service;");
                }

                if (basicSettings.getAvailableTo() != null) {
                    errorMessage.append("basicSettings.availableTo-[Available To] must be null for individual service;");
                }
            } else {
                if (StringUtils.isNotEmpty(basicSettings.getCustomerIdentifier())) {
                    errorMessage.append("basicSettings.customerIdentifier-Customer identifier must be blank for non-individual service;");
                }

                if (StringUtils.isEmpty(basicSettings.getName())) {
                    errorMessage.append("basicSettings.name-Name must not be blank;");
                }

                if (StringUtils.isEmpty(basicSettings.getNameTransliterated())) {
                    errorMessage.append("basicSettings.nameTransliterated-Transliterated Name must not be blank;");
                }
                if (basicSettings.getServiceGroupId() == null) {
                    errorMessage.append("basicSettings.serviceGroupId-Service Group must not be null for non individual service;");
                }

                if (StringUtils.isEmpty(basicSettings.getShortDescription())) {
                    errorMessage.append("basicSettings.shortDescription-Must not be empty for non individual service;");
                }
            }

            validateSaleChannels(basicSettings, errorMessage);
            validateSaleAreas(basicSettings, errorMessage);
            validateSegments(basicSettings, errorMessage);
            validateVatRate(basicSettings, errorMessage);

            if (!errorMessage.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }

        private static void validateSaleChannels(ServiceBasicSettingsRequest basicSettings, StringBuilder errorMessage) {
            if (BooleanUtils.isTrue(basicSettings.getIsIndividual())) {
                if (CollectionUtils.isNotEmpty(basicSettings.getSalesChannels()) || BooleanUtils.isTrue(basicSettings.getGlobalSalesChannel())) {
                    errorMessage.append("basicSettings.salesChannels-Sales Channels are disabled for individual service;");
                }
            } else {
                if (basicSettings.getGlobalSalesChannel() == null) {
                    errorMessage.append("basicSettings.globalSalesChannel-Global Sales Channel must not be null;");
                } else {
                    if (basicSettings.getGlobalSalesChannel() && basicSettings.getSalesChannels() != null) {
                        errorMessage.append("basicSettings.salesChannels-Sales Channel IDs must be null while [globalSalesChannel] is true;");
                    } else if (!basicSettings.getGlobalSalesChannel() && CollectionUtils.isEmpty(basicSettings.getSalesChannels())) {
                        errorMessage.append("basicSettings.salesChannels-Sales Channel IDs must contain at least one object while [globalSalesChannel] is false;");
                    }
                }
            }
        }

        private static void validateSaleAreas(ServiceBasicSettingsRequest basicSettings, StringBuilder errorMessage) {
            if (BooleanUtils.isTrue(basicSettings.getIsIndividual())) {
                if (CollectionUtils.isNotEmpty(basicSettings.getSalesAreas()) || BooleanUtils.isTrue(basicSettings.getGlobalSalesAreas())) {
                    errorMessage.append("basicSettings.salesAreas-Sales Areas are disabled for individual service;");
                }
            } else {
                if (basicSettings.getGlobalSalesAreas() == null) {
                    errorMessage.append("basicSettings.globalSalesArea-Global Sales Area must not be null;");
                } else {
                    if (basicSettings.getGlobalSalesAreas() && basicSettings.getSalesAreas() != null) {
                        errorMessage.append("basicSettings.salesAreas-Sales Areas IDs must be null while [globalSalesAreas] is true;");
                    } else if (!basicSettings.getGlobalSalesAreas() && CollectionUtils.isEmpty(basicSettings.getSalesAreas())) {
                        errorMessage.append("basicSettings.salesAreas-Sales Area IDs must contain at least one object while [globalSalesAreas] is false;");
                    }
                }
            }
        }

        private static void validateSegments(ServiceBasicSettingsRequest basicSettings, StringBuilder errorMessage) {
            if (BooleanUtils.isTrue(basicSettings.getIsIndividual())) {
                if (CollectionUtils.isNotEmpty(basicSettings.getSegments()) || BooleanUtils.isTrue(basicSettings.getGlobalSegment())) {
                    errorMessage.append("basicSettings.segments-Segments are disabled for individual service;");
                }
            } else {
                if (basicSettings.getGlobalSegment() == null) {
                    errorMessage.append("basicSettings.globalSegment-Global segments must not be null;");
                } else {
                    if (basicSettings.getGlobalSegment() && basicSettings.getSegments() != null) {
                        errorMessage.append("basicSettings.segments-Segment IDs must be null while [globalSegment] is true;");
                    } else if (!basicSettings.getGlobalSegment() && CollectionUtils.isEmpty(basicSettings.getSegments())) {
                        errorMessage.append("basicSettings.segments-Segment IDs must contain at least one object while [globalSegment] is false;");
                    }
                }
            }
        }

        private static void validateVatRate(ServiceBasicSettingsRequest request, StringBuilder validationMessageBuilder) {
            if (request.getGlobalVatRate() == null) {
                validationMessageBuilder.append("basicSettings.globalVatRate-Global Vat Rate must not be null;");
            } else {
                if (request.getGlobalVatRate() && request.getVatRateId() != null) {
                    validationMessageBuilder.append("basicSettings.vatRateId-Vat Rate ID must be null while [globalVatRate] is true;");
                } else if (!request.getGlobalVatRate() && request.getVatRateId() == null) {
                    validationMessageBuilder.append("basicSettings.vatRateId-Vat Rate ID must not be null while [globalVatRate] is false;");
                }
            }
        }

    }

}
