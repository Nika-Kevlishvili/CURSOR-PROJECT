package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.enums.product.service.ServicePaymentMethod;
import bg.energo.phoenix.model.enums.product.service.ServicePeriodicity;
import bg.energo.phoenix.model.enums.product.service.ServiceSaleMethod;
import bg.energo.phoenix.model.request.product.service.*;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.BaseServiceContractTermRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceTerms.ServiceTermsValidator.class, ValidServiceTerms.ServiceTermsValidatorEdit.class})
public @interface ValidServiceTerms {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceTermsValidator implements ConstraintValidator<ValidServiceTerms, CreateServiceRequest> {

        @Override
        public boolean isValid(CreateServiceRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessageBuilder = new StringBuilder();

            Long termId = request.getTerm();
            Long termGroupId = request.getTermGroup();
            ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();
            ServicePriceSettingsRequest priceSettings = request.getPriceSettings();
            ServiceAdditionalSettingsRequest additionalSettings = request.getAdditionalSettings();

            if (additionalSettings != null && additionalSettings.getPaymentMethod() != null) {
                ServicePaymentMethod servicePaymentMethod = additionalSettings.getPaymentMethod();
                if (servicePaymentMethod.equals(ServicePaymentMethod.FREE)) {
                    if (priceSettings != null) {
                        if (priceSettings.getEqualMonthlyInstallmentsActivation()) {
                            validationMessageBuilder.append("priceSettings.equalMonthlyInstallmentsActivation-[equalMonthlyInstallmentsActivation] must be false while service payment method is free;");
                        }
                    }

                    if (CollectionUtils.isNotEmpty(request.getPriceComponentGroups())) {
                        validationMessageBuilder.append("priceComponentGroups-[priceComponentGroups] price component groups must be empty while selected payment method is free;");
                    }

                    if (CollectionUtils.isNotEmpty(request.getPriceComponents())) {
                        validationMessageBuilder.append("priceComponents-[priceComponents] price components must be empty while selected payment method is free;");
                    }
                }

                if (servicePaymentMethod.equals(ServicePaymentMethod.FREE) || servicePaymentMethod.equals(ServicePaymentMethod.PERIODICAL)) {
                    Boolean paymentBeforeExecution = additionalSettings.getPaymentBeforeExecution();
                    if (paymentBeforeExecution != null && paymentBeforeExecution) {
                        validationMessageBuilder.append("additionalSettings.paymentBeforeExecution-[paymentBeforeExecution] must be disabled while method of payment is free or periodical;");
                    }
                }
            }

            if (basicSettings != null && CollectionUtils.isNotEmpty(basicSettings.getSaleMethods())) {
                if (additionalSettings!= null && basicSettings.getSaleMethods().contains(ServiceSaleMethod.ORDER) && additionalSettings.getPeriodicity() != null && additionalSettings.getPeriodicity().equals(ServicePeriodicity.SUBSCRIPTION) && (request.getContractTerms() == null || request.getContractTerms().isEmpty()))
                    validationMessageBuilder.append("createServiceRequest.contract/Subscription term-[contract/Subscription term] is mandatory;");

                if (basicSettings.getSaleMethods().contains(ServiceSaleMethod.CONTRACT)) {
                    if (termId == null && termGroupId == null) {
                        validationMessageBuilder.append("term-[Term Group ID] or [Term ID] must be defined;");
                        validationMessageBuilder.append("termGroup-[Term Group ID] or [Term ID] must be defined;");
                    }

                    if (termId != null && termGroupId != null) {
                        validationMessageBuilder.append("term-Either [Term Group IDs] or [Term IDs] must be defined;");
                        validationMessageBuilder.append("termGroup-Either [Term Group IDs] or [Term IDs] must be defined;");
                    }

                    if (CollectionUtils.isEmpty(request.getContractTerms())) {
                        validationMessageBuilder.append("contractTerms-Contract terms can not be empty;");
                    }
                } else {
                    ServicePriceSettingsRequest priceSettingsRequest = request.getPriceSettings();
                    if (priceSettingsRequest != null) {
                        Boolean monthlyInstallmentsActivation = priceSettingsRequest.getEqualMonthlyInstallmentsActivation();
                        if (monthlyInstallmentsActivation != null && monthlyInstallmentsActivation) {
                            validationMessageBuilder.append("priceSettings.equalMonthlyInstallmentsActivation-[equalMonthlyInstallmentsActivation] must be disabled while method of sale contract is not selected;");
                        }
                    }

                    if (CollectionUtils.isNotEmpty(request.getContractTerms())) {
                        boolean isPerpetuityCauseChecked = request
                                .getContractTerms()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(BaseServiceContractTermRequest::getPerpetuityCause)
                                .filter(Objects::nonNull)
                                .anyMatch(b -> b);

                        if (isPerpetuityCauseChecked) {
                            validationMessageBuilder.append("contractTerms.perpetuityCause-[perpetuityCause] Perpetuity shouldn't be checked in contract term when service is created for order;");
                        }
                    }

                }
            }


            boolean isValid = validationMessageBuilder.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }

    class ServiceTermsValidatorEdit implements ConstraintValidator<ValidServiceTerms, EditServiceRequest> {

        @Override
        public boolean isValid(EditServiceRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessageBuilder = new StringBuilder();

            Long termId = request.getTerm();
            Long termGroupId = request.getTermGroup();
            ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();
            ServicePriceSettingsRequest priceSettings = request.getPriceSettings();
            ServiceAdditionalSettingsRequest additionalSettings = request.getAdditionalSettings();

            if (additionalSettings != null && additionalSettings.getPaymentMethod() != null) {
                ServicePaymentMethod servicePaymentMethod = additionalSettings.getPaymentMethod();
                if (servicePaymentMethod.equals(ServicePaymentMethod.FREE)) {

                    if (priceSettings != null) {
                        if (priceSettings.getEqualMonthlyInstallmentsActivation()) {
                            validationMessageBuilder.append("priceSettings.equalMonthlyInstallmentsActivation-[equalMonthlyInstallmentsActivation] must be false while service payment method is free;");
                        }
                    }

                    if (CollectionUtils.isNotEmpty(request.getPriceComponentGroups())) {
                        validationMessageBuilder.append("priceComponentGroups-[priceComponentGroups] price component groups must be empty while selected payment method is free;");
                    }

                    if (CollectionUtils.isNotEmpty(request.getPriceComponents())) {
                        validationMessageBuilder.append("priceComponents-[priceComponents] price components must be empty while selected payment method is free;");
                    }

                }

                if (servicePaymentMethod.equals(ServicePaymentMethod.FREE) || servicePaymentMethod.equals(ServicePaymentMethod.PERIODICAL)) {
                    Boolean paymentBeforeExecution = additionalSettings.getPaymentBeforeExecution();
                    if (paymentBeforeExecution != null && paymentBeforeExecution) {
                        validationMessageBuilder.append("additionalSettings.paymentBeforeExecution-[paymentBeforeExecution] must be disabled while method of payment is free or periodical;");
                    }
                }
            }

            if (basicSettings != null && CollectionUtils.isNotEmpty(basicSettings.getSaleMethods())) {
                if (additionalSettings!= null && basicSettings.getSaleMethods().contains(ServiceSaleMethod.ORDER) && additionalSettings.getPeriodicity() != null && additionalSettings.getPeriodicity().equals(ServicePeriodicity.SUBSCRIPTION) && (request.getContractTerms() == null || request.getContractTerms().isEmpty()))
                    validationMessageBuilder.append("createServiceRequest.contract/Subscription term-[contract/Subscription term] is mandatory;");

                if (basicSettings.getSaleMethods().contains(ServiceSaleMethod.CONTRACT)) {
                    if (termId == null && termGroupId == null) {
                        validationMessageBuilder.append("term-[Term Group ID] or [Term ID] must be defined;");
                        validationMessageBuilder.append("termGroup-[Term Group ID] or [Term ID] must be defined;");
                    }

                    if (termId != null && termGroupId != null) {
                        validationMessageBuilder.append("term-Either [Term Group IDs] or [Term IDs] must be defined;");
                        validationMessageBuilder.append("termGroup-Either [Term Group IDs] or [Term IDs] must be defined;");
                    }

                    if (CollectionUtils.isEmpty(request.getContractTerms())) {
                        validationMessageBuilder.append("contractTerms-Contract terms can not be empty;");
                    }
                } else {
                    ServicePriceSettingsRequest priceSettingsRequest = request.getPriceSettings();
                    if (priceSettingsRequest != null) {
                        Boolean monthlyInstallmentsActivation = priceSettingsRequest.getEqualMonthlyInstallmentsActivation();
                        if (monthlyInstallmentsActivation != null && monthlyInstallmentsActivation) {
                            validationMessageBuilder.append("priceSettings.equalMonthlyInstallmentsActivation-[equalMonthlyInstallmentsActivation] must be disabled while method of sale contract is not selected;");
                        }
                    }

                    if (CollectionUtils.isNotEmpty(request.getContractTerms())) {
                        boolean isPerpetuityCauseChecked = request
                                .getContractTerms()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(BaseServiceContractTermRequest::getPerpetuityCause)
                                .filter(Objects::nonNull)
                                .anyMatch(b -> b);

                        if (isPerpetuityCauseChecked) {
                            validationMessageBuilder.append("contractTerms.perpetuityCause-[perpetuityCause] Perpetuity shouldn't be checked in contract term when service is created for order;");
                        }
                    }

                }
            }


            boolean isValid = validationMessageBuilder.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }

}
