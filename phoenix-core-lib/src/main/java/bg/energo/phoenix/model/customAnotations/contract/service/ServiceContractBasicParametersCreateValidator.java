package bg.energo.phoenix.model.customAnotations.contract.service;

import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractCreateRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static bg.energo.phoenix.util.contract.ContractValidationsUtil.serviceContractDatesValidation;
import static bg.energo.phoenix.util.contract.ContractValidationsUtil.validateServiceContractSigningDate;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceContractBasicParametersCreateValidator.ServiceContractBasicParametersCreateValidatorImpl.class})
public @interface ServiceContractBasicParametersCreateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractBasicParametersCreateValidatorImpl implements ConstraintValidator<ServiceContractBasicParametersCreateValidator, ServiceContractCreateRequest> {
        @Override
        public boolean isValid(ServiceContractCreateRequest mainRequest, ConstraintValidatorContext context) {
            ServiceContractBasicParametersCreateRequest request = mainRequest.getBasicParameters();
            ServiceContractServiceParametersCreateRequest serviceParameters = mainRequest.getServiceParameters();
            BigDecimal contractTermUntilAmountIsReached = request.getContractTermUntilAmountIsReached();
            Boolean contractTermUntilAmountIsReachedCheckbox = request.getContractTermUntilAmountIsReachedCheckbox();
            Long currencyId = request.getCurrencyId();
            LocalDate contractTermEndDate = null;
            if (serviceParameters != null) {
                contractTermEndDate = serviceParameters.getContractTermEndDate();
            }
            if (contractTermUntilAmountIsReachedCheckbox == null) {
                context.buildConstraintViolationWithTemplate("basicParameters.contractTermUntilAmountIsReachedCheckbox-is mandatory;").addConstraintViolation();
                return false;
            }
            if (contractTermUntilAmountIsReachedCheckbox) {
                if (contractTermUntilAmountIsReached == null) {
                    context.buildConstraintViolationWithTemplate("basicParameters.contractTermUntilAmountIsReachedCheckbox-is mandatory when " +
                            "contractTermUntilAmountIsReachedCheckbox is selected;").addConstraintViolation();
                    return false;
                }
                if (currencyId == null) {
                    context.buildConstraintViolationWithTemplate("basicParameters.currencyId-is mandatory when " +
                            "contractTermUntilAmountIsReachedCheckbox is selected;").addConstraintViolation();
                    return false;
                }
            }

            LocalDate signingDate = request.getSignInDate();
            LocalDate entryIntoForceDate = request.getEntryIntoForceDate();
            LocalDate contractTermStartDate = request.getStartOfTheInitialTermOfTheContract();

            StringBuilder sb = new StringBuilder();

            serviceContractDatesValidation(sb, request.getEntryIntoForceDate(), request.getStartOfTheInitialTermOfTheContract(), request.getContractTermEndDate(), request.getContractStatus());
            validateServiceContractSigningDate(signingDate, sb, request.getContractStatus(), request.getDetailsSubStatus());
            if (!sb.isEmpty()) {
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            if (signingDate != null) {
                if (Boolean.FALSE.equals(EPBDateUtils.isDateInRange(signingDate, LocalDate.of(1990, 1, 1), LocalDate.now()))) {
                    context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be between 01/01/1990 and %s;".formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))).addConstraintViolation();
                    return false;
                } else {
                    if (entryIntoForceDate != null) {
                        if (signingDate.isAfter(entryIntoForceDate)) {
                            context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be less basicParameters.entryIntoForceDate;").addConstraintViolation();
                            return false;
                        }
                    }

                    if (contractTermStartDate != null) {
                        if (signingDate.isAfter(contractTermStartDate)) {
                            context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be less then ServiceContractServiceParametersCreateRequest.startOfTheInitialTermOfTheContract;").addConstraintViolation();
                            return false;
                        }
                    }
                }
            }

            if (entryIntoForceDate != null) {
                if (Boolean.FALSE.equals(EPBDateUtils.isDateInRange(entryIntoForceDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31)))) {
                    context.buildConstraintViolationWithTemplate("basicParameters.entryIntoForceDate-[entryIntoForceDate] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if (signingDate != null) {
                    if (entryIntoForceDate.isBefore(signingDate)) {
                        context.buildConstraintViolationWithTemplate("basicParameters.entryIntoForceDate-[entryIntoForceDate] should be more or equal;").addConstraintViolation();
                        return false;
                    }
                }
                if (contractTermEndDate != null) {
                    if (!entryIntoForceDate.equals(serviceParameters.getContractTermEndDate())) {
                        if (!entryIntoForceDate.isBefore(serviceParameters.getContractTermEndDate())) {
                            context.buildConstraintViolationWithTemplate("basicParameters.entryIntoForceDate-[entryIntoForceDate] should be less or equal to contract Term End Date;").addConstraintViolation();
                            return false;
                        }
                    }
                }
            }

            if (contractTermStartDate != null) {
                if (!EPBDateUtils.isDateInRange(contractTermStartDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("basicParameters.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if (signingDate != null) {
                    if (contractTermStartDate.isBefore(signingDate)) {
                        context.buildConstraintViolationWithTemplate("basicParameters.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be more or equal to basicParameters.signInDate;").addConstraintViolation();
                        return false;
                    }
                }
                if (contractTermEndDate != null) {
                    if (contractTermStartDate.isAfter(contractTermEndDate)) {
                        context.buildConstraintViolationWithTemplate("basicParameters.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be less or equal to basicParameters.contractTermEndDate;").addConstraintViolation();
                        return false;
                    }
                }
            }

            if (contractTermEndDate != null) {
                if (!EPBDateUtils.isDateInRange(contractTermEndDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("basicParameters.contractTermEndDate-[contractTermEndDate] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if (signingDate != null && contractTermEndDate.isBefore(signingDate)) {
                    context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be less or equal to contractTermEndDate;").addConstraintViolation();
                    return false;
                }
                if (contractTermStartDate != null && contractTermEndDate.isBefore(contractTermStartDate)) {
                    context.buildConstraintViolationWithTemplate("serviceParameters.contractTermEndDate-[contractTermEndDate] should be more or equal to basicParameters.contractTermStartDate;").addConstraintViolation();
                    return false;
                }
            }
            if (serviceParameters != null) {
                LocalDate thirdTabEntryIntoForceDate = serviceParameters.getEntryIntoForceDate();
                LocalDate thirdTabStartOfInitialTerm = serviceParameters.getStartOfContractInitialTermDate();
                LocalDate firtsTabSignInDate = request.getSignInDate();

                if (thirdTabStartOfInitialTerm != null && firtsTabSignInDate != null) {
                    if (firtsTabSignInDate.isAfter(thirdTabStartOfInitialTerm)) {
                        context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be less or equal to startOfContractInitialTermDate;").addConstraintViolation();
                        return false;
                    }
                }

                if (thirdTabEntryIntoForceDate != null && firtsTabSignInDate != null) {
                    if (firtsTabSignInDate.isAfter(thirdTabEntryIntoForceDate)) {
                        context.buildConstraintViolationWithTemplate("basicParameters.signInDate-[signInDate] should be less or equal to entryIntoForceDate;").addConstraintViolation();
                        return false;
                    }
                }

                if (thirdTabStartOfInitialTerm != null && thirdTabEntryIntoForceDate != null) {
                    if (thirdTabStartOfInitialTerm.isAfter(thirdTabEntryIntoForceDate)) {
                        context.buildConstraintViolationWithTemplate("serviceParameters.startOfContractInitialTermDate-[startOfContractInitialTermDate] should be less or equal to entryIntoForceDate;").addConstraintViolation();
                        return false;
                    }
                }
            }


            return true;
        }
    }
}
