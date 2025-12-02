package bg.energo.phoenix.model.customAnotations.contract.service.edit;

import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractBasicParametersEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractServiceParametersEditRequest;
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
import java.util.Objects;

import static bg.energo.phoenix.util.contract.ContractValidationsUtil.serviceContractDatesValidation;
import static bg.energo.phoenix.util.contract.ContractValidationsUtil.validateServiceContractSigningDate;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceContractBasicParametersEditValidator.ServiceContractBasicParametersEditValidatorImpl.class})
public @interface ServiceContractBasicParametersEditValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractBasicParametersEditValidatorImpl implements ConstraintValidator<ServiceContractBasicParametersEditValidator, ServiceContractEditRequest> {
        @Override
        public boolean isValid(ServiceContractEditRequest mainRequest, ConstraintValidatorContext context) {
            ServiceContractBasicParametersEditRequest request = mainRequest.getBasicParameters();
            ServiceContractServiceParametersEditRequest serviceParameters = mainRequest.getServiceParameters();
            BigDecimal contractTermUntilAmountIsReached = request.getContractTermUntilAmountIsReached();
            Boolean contractTermUntilAmountIsReachedCheckbox = request.getContractTermUntilAmountIsReachedCheckbox();
            Long currencyId = request.getCurrencyId();
            LocalDate contractTermEndDate = null;
            if (serviceParameters != null) {
                contractTermEndDate = serviceParameters.getContractTermEndDate();
            }
            if (contractTermUntilAmountIsReachedCheckbox == null) {
                context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermUntilAmountIsReachedCheckbox-is mandatory;").addConstraintViolation();
                return false;
            }
            if (contractTermUntilAmountIsReachedCheckbox) {
                if (contractTermUntilAmountIsReached == null) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermUntilAmountIsReachedCheckbox-is mandatory when " +
                            "contractTermUntilAmountIsReachedCheckbox is selected;").addConstraintViolation();
                    return false;
                }
                if (currencyId == null) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.currencyId-is mandatory when " +
                            "contractTermUntilAmountIsReachedCheckbox is selected;").addConstraintViolation();
                    return false;
                }
            }
            LocalDate signingDate = request.getSignInDate();
            LocalDate entryIntoForceDate = request.getEntryIntoForceDate();
            LocalDate contractTermStartDate = request.getStartOfTheInitialTermOfTheContract();
            LocalDate terminationDate = request.getTerminationDate();
            LocalDate perpetuityDate = request.getPerpetuityDate();
            LocalDate firstTabContractTermEndDate = request.getContractTermEndDate();
            ServiceContractDetailStatus contractStatus = request.getContractStatus();

            StringBuilder sb = new StringBuilder();

            serviceContractDatesValidation(sb, request.getEntryIntoForceDate(), request.getStartOfTheInitialTermOfTheContract(), request.getContractTermEndDate(), request.getContractStatus());
            validateAdditionalDates(sb, request);
            validateServiceContractSigningDate(signingDate, sb, request.getContractStatus(), request.getDetailsSubStatus());
            if (!sb.isEmpty()) {
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            if (mainRequest.getServiceParameters() != null) {
                if (!(request.getEntryIntoForceDate() == null &&
                        mainRequest.getServiceParameters().getStartOfContractInitialTermDate() == null &&
                        request.getPerpetuityDate() == null &&
                        request.getContractTermEndDate() == null &&
                        request.getTerminationDate() == null &&
                        contractStatus != ServiceContractDetailStatus.SIGNED &&
                        contractStatus != ServiceContractDetailStatus.ACTIVE_IN_TERM &&
                        contractStatus != ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY &&
                        contractStatus != ServiceContractDetailStatus.ENTERED_INTO_FORCE)) {
                    ServiceContractDetailsSubStatus detailsSubStatus = request.getDetailsSubStatus();
                    if (detailsSubStatus.equals(ServiceContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES) || detailsSubStatus.equals(ServiceContractDetailsSubStatus.SPECIAL_PROCESSES)) {
                        if (signingDate == null) {
                            context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be present;").addConstraintViolation();
                            return false;
                        }
                    }
                }
            } else {
                context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.serviceParameters-[serviceParameters] should be present;").addConstraintViolation();
                return false;
            }
            if (contractStatus.equals(ServiceContractDetailStatus.ENTERED_INTO_FORCE) ||
                    contractStatus.equals(ServiceContractDetailStatus.ACTIVE_IN_TERM) ||
                    contractStatus.equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY) ||
                    contractStatus.equals(ServiceContractDetailStatus.TERMINATED)) {
                if (entryIntoForceDate == null) {
                    context.buildConstraintViolationWithTemplate("asicParameters.entryIntoForceDate-[entryIntoForceDate] should be present;").addConstraintViolation();
                    return false;
                }
            }

            if (signingDate != null) {
                if (!EPBDateUtils.isDateInRange(signingDate, LocalDate.of(1990, 1, 1), LocalDate.now())) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be between 01/01/1990 and %s;".formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))).addConstraintViolation();
                    return false;
                } else {
                    if (entryIntoForceDate != null) {
                        if (signingDate.isAfter(entryIntoForceDate)) {
                            context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be after Entry Into Force Date: [%s];".formatted(entryIntoForceDate)).addConstraintViolation();
                            return false;
                        }
                    }

                    if (contractTermStartDate != null) {
                        if (signingDate.isAfter(contractTermStartDate)) {
                            context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be less then Start Of The Initial Term: [%s];".formatted(request.getStartOfTheInitialTermOfTheContract())).addConstraintViolation();
                            return false;
                        }
                    }
                }
                if (terminationDate != null) {
                    if (!signingDate.isBefore(terminationDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be less then Termination Date: [%s];".formatted(terminationDate)).addConstraintViolation();
                        return false;
                    }
                }
                if (perpetuityDate != null) {
                    if (!signingDate.isBefore(perpetuityDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.signInDate-[signInDate] should be less then Perpetuity Date: [%s];".formatted(perpetuityDate)).addConstraintViolation();
                        return false;
                    }
                    LocalDate now = LocalDate.now();
                    if (perpetuityDate.isAfter(now)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.perpetuityDate-[perpetuityDate] should be past or current day;").addConstraintViolation();
                        return false;
                    }
                }
                if (contractTermEndDate != null) {
                    if (contractTermEndDate.isBefore(signingDate)) {
                        context.buildConstraintViolationWithTemplate("serviceParameters.contractTermEndDate-[contractTermEndDate] should be less then Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                        return false;
                    }
                }
            }

            if (entryIntoForceDate != null) {
                if (!EPBDateUtils.isDateInRange(entryIntoForceDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if (signingDate != null) {
                    if (entryIntoForceDate.isBefore(signingDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be more or equal Sign In Date: [%s];".formatted(signingDate)).addConstraintViolation();
                        return false;
                    }
                }
                if (contractTermEndDate != null) {
                    if (!entryIntoForceDate.equals(serviceParameters.getContractTermEndDate())) {
                        if (!entryIntoForceDate.isBefore(serviceParameters.getContractTermEndDate())) {
                            context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be less or equal to Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                            return false;
                        }
                    }
                }
                if (terminationDate != null) {
                    if (entryIntoForceDate.isAfter(terminationDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be less or equal to Termination Date: [%s];".formatted(terminationDate)).addConstraintViolation();
                        return false;
                    }
                }
                if (contractTermEndDate != null) {
                    if (!entryIntoForceDate.equals(contractTermEndDate)) {
                        if (entryIntoForceDate.isAfter(contractTermEndDate)) {
                            context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be less or equal to Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                            return false;
                        }
                    }
                }
                if (perpetuityDate != null) {
                    if (entryIntoForceDate.isAfter(perpetuityDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] should be less or equal to Perpetuity Date: [%s];".formatted(perpetuityDate)).addConstraintViolation();
                        return false;
                    }
                }
            }
            if (contractTermStartDate != null) {
                if (!EPBDateUtils.isDateInRange(contractTermStartDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if(signingDate != null){
                    if (contractTermStartDate.isBefore(signingDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be more or equal to Sign In Date: [%s];".formatted(signingDate)).addConstraintViolation();
                        return false;
                    }
                }

                if (contractTermStartDate != null && contractTermEndDate != null) {
                    if (contractTermStartDate.isAfter(contractTermEndDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be less or equal to Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                        return false;
                    }
                }
                if (terminationDate != null) {
                    if (contractTermStartDate.isAfter(terminationDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be less or equal to Termination Date: [%s];".formatted(terminationDate)).addConstraintViolation();
                        return false;
                    }
                }
                if (perpetuityDate != null) {
                    if (contractTermStartDate.isAfter(perpetuityDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startOfTheInitialTermOfTheContract-[startOfTheInitialTermOfTheContract] should be less or equal to Perpetuity Date: [%s];".formatted(perpetuityDate)).addConstraintViolation();
                        return false;
                    }
                }

            }
            if (contractTermEndDate != null) {
                if (!EPBDateUtils.isDateInRange(contractTermEndDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermEndDate-[contractTermEndDate] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
                if (signingDate != null && contractTermEndDate.isBefore(signingDate)) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermEndDate-[contractTermEndDate] should be more or equal to Sing In Date: [%s];".formatted(signingDate)).addConstraintViolation();
                    return false;
                }
                if (contractTermStartDate != null && contractTermEndDate.isBefore(contractTermStartDate)) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermEndDate-[contractTermEndDate] should be more or equal to Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                    return false;
                }

                if (perpetuityDate != null) {
                    if (!perpetuityDate.isBefore(contractTermEndDate)) {
                        context.buildConstraintViolationWithTemplate("serviceContractServiceParametersCreateRequest.perpetuityDate-[perpetuityDate] should be less or equal to Contract Term End Date: [%s];".formatted(contractTermEndDate)).addConstraintViolation();
                        return false;
                    }
                }
            }
            if (mainRequest.getBasicParameters().getStartDate() != null) {
                if (!EPBDateUtils.isDateInRange(mainRequest.getBasicParameters().getStartDate(), LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractTermEndDate-[contractTermEndDate] should be between 01/01/1990 and 31/12/2090;").addConstraintViolation();
                    return false;
                }
            } else {
                context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.startDate-[startDate] is mandatory;").addConstraintViolation();
                return false;
            }
            if (terminationDate != null) {
                LocalDate now = LocalDate.now();
                if (terminationDate.isAfter(now)) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.terminationDate-[terminationDate] should be past or current day;").addConstraintViolation();
                    return false;
                }

            }
            if (terminationDate != null && perpetuityDate != null) {
                if (!terminationDate.isAfter(perpetuityDate)) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.terminationDate-[terminationDate] should be after Perpetuity Date: [%s];".formatted(perpetuityDate)).addConstraintViolation();
                    return false;
                }
            }
            if (request.getContractStatus().equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY)) {
                if (request.getPerpetuityDate() == null) {
                    context.buildConstraintViolationWithTemplate("serviceContractBasicParametersCreateRequest.contractStatus-[contractStatus] when contract Status is ACTIVE_IN_PERPETUITY perpetuity date shouldn't be null;").addConstraintViolation();
                    return false;
                }
            }
            return true;
        }

        private void validateAdditionalDates(StringBuilder sb, ServiceContractBasicParametersEditRequest request) {
            if (Objects.equals(request.getContractStatus(), ServiceContractDetailStatus.DRAFT)) {
                if (request.getActivationDate() != null) {
                    sb.append("basicParameters.activationDate-activationDate should not be present when contract status is DRAFT;");
                }
                if (request.getPerpetuityDate() != null) {
                    sb.append("basicParameters.perpetuityDate-perpetuityDate should not be present when contract status is DRAFT;");
                }
                if(request.getTerminationDate() != null) {
                    sb.append("basicParameters.terminationDate-terminationDate should not be present when contract status is DRAFT;");
                }
            }
        }
    }
}
