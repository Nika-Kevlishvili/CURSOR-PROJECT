package bg.energo.phoenix.util.contract;

import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

public class ContractValidationsUtil {


//----------------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------------

    public static void serviceContractDatesValidation(StringBuilder errorMessages, LocalDate entryIntoForceDate, LocalDate startOfTheInitialTermOfTheContract, LocalDate contractTermEndDate, ServiceContractDetailStatus contractDetailsStatus) {
        if (Objects.equals(contractDetailsStatus, ServiceContractDetailStatus.DRAFT)) {
            validateOnDraftStatus(entryIntoForceDate, startOfTheInitialTermOfTheContract, contractTermEndDate, "serviceParameters", errorMessages);
        } else {
            if (entryIntoForceDate != null) {
                validateServiceContractEntryInForce(entryIntoForceDate, contractDetailsStatus, contractDetailsStatus,errorMessages);
            }
        }

    }

    public static void validateServiceContractSigningDate(LocalDate signingDate, StringBuilder sb, ServiceContractDetailStatus status, ServiceContractDetailsSubStatus subStatus) {
        if (signingDate == null) {
            boolean isValidStatus = isServiceContractValidStatus(status, subStatus);

            if (isValidStatus) {
                sb.append("basicParameters.signingDate-Signing date must not be null;");
            }
        } else {
            boolean isDraftOrReady = Arrays.asList(ServiceContractDetailStatus.DRAFT, ServiceContractDetailStatus.READY).contains(status);
            boolean isSignedByCustomerOrEPres = Objects.equals(status, ServiceContractDetailStatus.SIGNED) &&
                    Arrays.asList(ServiceContractDetailsSubStatus.SIGNED_BY_CUSTOMER, ServiceContractDetailsSubStatus.SIGNED_BY_EPRES).contains(subStatus);


            if (isDraftOrReady || isSignedByCustomerOrEPres) {
                sb.append("basicParameters.signingDate-Signing Date must not be present;");
            }

            if (signingDate.isAfter(LocalDate.now())) {
                sb.append("basicParameters.signingDate-Signing Date must not be in the future;");
            }
        }
    }

    private static boolean isServiceContractValidStatus(ServiceContractDetailStatus status, ServiceContractDetailsSubStatus subStatus) {
        return (Objects.equals(status, ServiceContractDetailStatus.SIGNED) &&
                (Objects.equals(subStatus, ServiceContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES) ||
                        Objects.equals(subStatus, ServiceContractDetailsSubStatus.SPECIAL_PROCESSES))) ||
                (Arrays.asList(ServiceContractDetailStatus.TERMINATED, ServiceContractDetailStatus.ACTIVE_IN_TERM, ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY).contains(status));
    }


    private static void validateServiceContractEntryInForce(LocalDate entryIntoForceDate, ServiceContractDetailStatus status, ServiceContractDetailStatus contractDetailsStatus, StringBuilder errorMessages) {
        if (entryIntoForceDate.equals(LocalDate.now()) ||
                entryIntoForceDate.isBefore(LocalDate.now())) {
            if (!Arrays.asList(ServiceContractDetailStatus.ENTERED_INTO_FORCE, ServiceContractDetailStatus.ACTIVE_IN_TERM,
                            ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY, ServiceContractDetailStatus.TERMINATED, ServiceContractDetailStatus.CANCELLED)
                    .contains(status)) {
                errorMessages.append("basicParameters.contractStatus-Contract Status is incorrect;");
            }
        } else {
            if (!Arrays.asList(ServiceContractDetailStatus.READY, ServiceContractDetailStatus.SIGNED).contains(status)) {
                errorMessages.append("basicParameters.contractStatus-Contract Status must be ready or signed when entry into force date is defined in the future;");
            }
        }
    }

    private static void validateOnDraftStatus(LocalDate entryInForceDate, LocalDate startOfInitialTerm, LocalDate contractTermEndDate, String requestName, StringBuilder errorMessages) {
        if (entryInForceDate != null) {
            errorMessages.append("basicParameters.entryInForceDate-entryInForceDate should not be present when contract status is DRAFT;");
        }
        if (startOfInitialTerm != null) {
            errorMessages.append("basicParameters.startOfInitialTerm-startOfInitialTerm should not be present when contract status is DRAFT;");
        }
        if (contractTermEndDate != null) {
            errorMessages.append(requestName).append(".contractTermEndDate-contractTermEndDate should not be present when contract status is DRAFT;");
        }
    }
}
