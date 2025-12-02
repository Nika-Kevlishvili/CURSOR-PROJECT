package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractThirdPageFields;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.ContractEntryIntoForceForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.StartOfContractInitialTermsForContractFields;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceContractDateService {
    @Value("${contract.without_term.value}")
    private String maxDate;

    //validates and sets dates
    public void validateDates(
            ServiceContracts contract,
            ServiceContractDetails contractDetails,
            LocalDate signingDate,
            LocalDate entryInForceDateFirst,
            LocalDate startOfInitialTerm,
            ServiceContractDetailStatus status,
            ServiceContractDetailsSubStatus subStatus,
            ServiceContractServiceParametersCreateRequest serviceParameters,
            ServiceContractThirdPageFields sourceView,
            List<String> errorMessages) {

        if (status.equals(ServiceContractDetailStatus.DRAFT)) {
            throwExceptionIfDatesNotNull(signingDate, entryInForceDateFirst, startOfInitialTerm, errorMessages);
        } else if (status.equals(ServiceContractDetailStatus.READY)) {
            if (signingDate != null) {
                errorMessages.add("basicParameters.signInDate-Signing date should be empty!;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryInForceDate-Entry in force should be in future!;");
            }
        } else if (List.of(ServiceContractDetailStatus.ENTERED_INTO_FORCE, ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY, ServiceContractDetailStatus.ACTIVE_IN_TERM, ServiceContractDetailStatus.TERMINATED)
                .contains(status)) {
            if (signingDate == null) {
                errorMessages.add("basicParameters.signInDate-Signing date is mandatory!;");
            } else if (signingDate.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.signingDate-Signing date should be today or in future;");
            }
            if (entryInForceDateFirst == null) {
                errorMessages.add("basicParameters.entryIntoForceDate-Entry in force is mandatory!;");
            } else {
                if (entryInForceDateFirst.isAfter(LocalDate.now())) {
                    errorMessages.add("basicParameters.entryIntoForceDate-Entry in force should be today or past!;");
                }
            }
        } else if (isInvalidSignedStatus(status, subStatus)) {
            if (signingDate != null) {
                errorMessages.add("basicParameters.signInDate-Signing date should be empty!;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryIntoForceDate-Entry in force should be in future!;");
            }
        } else if (status.equals(ServiceContractDetailStatus.SIGNED)) {
            if (signingDate == null) {
                errorMessages.add("basicParameters.signInDate-Signing date is mandatory!;");
            } else if (signingDate.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.signInDate-Signing date should be today or in future;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryIntoForceDate-Entry in force should be in future!;");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        contract.setSigningDate(signingDate);
        contract.setEntryIntoForceDate(entryInForceDateFirst);
        contract.setContractInitialTermStartDate(startOfInitialTerm);

        ContractEntryIntoForce entryIntoForce = serviceParameters.getEntryIntoForce();
        if (entryIntoForce.equals(ContractEntryIntoForce.SIGNING) && signingDate != null) {
            if (entryInForceDateFirst == null) {
                contract.setEntryIntoForceDate(signingDate);
            }
        } else if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            if (entryInForceDateFirst == null && !status.equals(ServiceContractDetailStatus.DRAFT)) {
                contract.setEntryIntoForceDate(serviceParameters.getEntryIntoForceDate());
            }
            if (List.of(ServiceContractDetailStatus.READY, ServiceContractDetailStatus.SIGNED).contains(status)) {
                if (!serviceParameters.getEntryIntoForceDate().isAfter(LocalDate.now())) {
                    errorMessages.add("serviceParameters.entryIntoForceDate-Entry in force date should be in feature!;");
                }
            }
            contractDetails.setEntryIntoForceValue(serviceParameters.getEntryIntoForceDate());
        }

        StartOfContractInitialTerm startOfContractInitialTerm = serviceParameters.getStartOfContractInitialTerm();
        if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.SIGNING) && signingDate != null) {
            if (startOfInitialTerm == null) {
                contract.setContractInitialTermStartDate(signingDate);
            }
            contractDetails.setInitialTermStartValue(signingDate);
        } else if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE)) {
            if (startOfInitialTerm == null && !status.equals(ServiceContractDetailStatus.DRAFT)) {
                contract.setContractInitialTermStartDate(serviceParameters.getStartOfContractInitialTermDate());
            }
            contractDetails.setInitialTermStartValue(serviceParameters.getStartOfContractInitialTermDate());
        }else if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.FIRST_DAY_MONTH_SIGNING) && signingDate != null) {
            int maxDay = YearMonth.now().lengthOfMonth();
            Integer contractDay = sourceView.getStartOfContractInitialTermsForContractFields().getFirstDayOfTheMonthOfInitialContractTerm();
            Integer day = contractDay>maxDay ? maxDay:contractDay;

            LocalDate calculatedDate = null;
            if (day >= signingDate.getDayOfMonth()) {
                LocalDate localDate = signingDate.plusMonths(1);

                calculatedDate=LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
            } else {
                LocalDate localDate = signingDate.plusMonths(2);
                calculatedDate=LocalDate.of(localDate.getYear(),localDate.getMonth(),1);
            }
            if(startOfInitialTerm==null){
                contract.setContractInitialTermStartDate(calculatedDate);
            }
            contractDetails.setInitialTermStartValue(calculatedDate);
        }
        contractDetails.setEntryIntoForce(entryIntoForce);
        contractDetails.setStartOfContractInitialTerm(startOfContractInitialTerm);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

    }

    //validates service term values;
    public void validateSourceView(ServiceContractThirdPageFields sourceView,
                                   ServiceContractServiceParametersCreateRequest serviceParameters,
                                   List<String> errorMessages
    ) {
        ContractEntryIntoForceForContractFields contractEntryIntoForces = sourceView.getContractEntryIntoForces();
        if (!contractEntryIntoForces.getContractEntryIntoForces().contains(serviceParameters.getEntryIntoForce())) {
            errorMessages.add("serviceParameters.entryIntoForceDate-Wrong Entry into force provided!;");
        } else if (serviceParameters.getEntryIntoForce().equals(ContractEntryIntoForce.EXACT_DAY)
                   && contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay() != null
                   &&
//                   !Objects.equals(serviceParameters.getEntryIntoForceDate().getDayOfMonth(), contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay())) {
                   !validateLastDayOfTheMonth(serviceParameters.getEntryIntoForceDate(),contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay())) {
            errorMessages.add("serviceParameters.entryIntoForceDate-Wrong Entry into force value provided!;");
        }

        StartOfContractInitialTermsForContractFields initialTermFields = sourceView.getStartOfContractInitialTermsForContractFields();
        if (initialTermFields.getStartsOfContractInitialTerms() != null) {
            if (!initialTermFields.getStartsOfContractInitialTerms().contains(serviceParameters.getStartOfContractInitialTerm())) {
                errorMessages.add("serviceParameters.startOfContractInitialTerm-Wrong Start Of Contract Initial Term provided!;");
            } else if (serviceParameters.getStartOfContractInitialTerm().equals(StartOfContractInitialTerm.EXACT_DATE)
                       && initialTermFields.getStartDayOfInitialContractTerm() != null
//                       && !Objects.equals(serviceParameters.getStartOfContractInitialTermDate().getDayOfMonth(), initialTermFields.getStartDayOfInitialContractTerm())) {
                       && !validateLastDayOfTheMonth(serviceParameters.getStartOfContractInitialTermDate(),initialTermFields.getStartDayOfInitialContractTerm())) {
                errorMessages.add("serviceParameters.startOfContractInitialTermDate-Wrong Start Of Contract Initial Term value provided!;");
            }
        }

    }
    private boolean validateLastDayOfTheMonth (LocalDate startOfTheContract, Integer startDayOfInitialContractTerm){
        List<Integer> lastDays = List.of(31,30,29);
        int lastDayOfContractStartMonth = startOfTheContract.with(lastDayOfMonth()).getDayOfMonth();
        if(!Objects.equals(startOfTheContract.getDayOfMonth(), startDayOfInitialContractTerm)
                && lastDays.contains(startDayOfInitialContractTerm)
                && startOfTheContract.getDayOfMonth() == lastDayOfContractStartMonth) {
            return true;
        } else return Objects.equals(startOfTheContract.getDayOfMonth(), startDayOfInitialContractTerm);
    }

    //validates contract term and sets value in details
    public LocalDate validateContractTerm(ServiceContracts contract,
                                          ServiceContractDetails contractDetails,
                                          ServiceContractThirdPageFields sourceView,
                                          ServiceContractServiceParametersCreateRequest serviceParameters,
                                          List<String> errorMessages) {
        List<ServiceContractTermShortResponse> serviceContractTerms = sourceView.getServiceContractTerms();
        Map<Long, ServiceContractTermShortResponse> collect = serviceContractTerms.stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        if (!collect.containsKey(serviceParameters.getContractTermId())) {
            errorMessages.add("serviceParameters.contractTermId-wrong contract term selected;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        ServiceContractTermShortResponse termsResponse = collect.get(serviceParameters.getContractTermId());
        if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE) && serviceParameters.getContractTermEndDate() == null) {
            errorMessages.add("serviceParameters.contractTermEndDate-should not be null;");
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        LocalDate termEndDate = calculateTermEndDate(termsResponse, contract.getContractInitialTermStartDate(), serviceParameters.getContractTermEndDate());
        contractDetails.setContractTermEndDate(termEndDate);

        return termEndDate;

    }

    //Used for update request
    public void setContractTermEndDate(ServiceContracts contract,
                                       ServiceContractDetailStatus status,
                                       LocalDate contractTermEndDateRequest,
                                       LocalDate termEndDate) {
        if (!status.equals(ServiceContractDetailStatus.DRAFT) && contractTermEndDateRequest == null) {
            contract.setContractTermEndDate(termEndDate);
        } else if (contractTermEndDateRequest != null) {
            contract.setContractTermEndDate(contractTermEndDateRequest);
        }
    }

    public void validatePerpetuity(ServiceContracts contract,
                                   ServiceContractThirdPageFields sourceView,
                                   ServiceContractDetailStatus status,
                                   LocalDate terminationDate,
                                   LocalDate perpetuityDate,
                                   ServiceContractServiceParametersCreateRequest serviceParameters,
                                   List<String> errorMessages
    ) {

        if (status.equals(ServiceContractDetailStatus.TERMINATED) && terminationDate == null) {
            errorMessages.add("basicParameters.terminationDate-termination  date should not be empty!;");
        } else if (!status.equals(ServiceContractDetailStatus.TERMINATED) && terminationDate != null) {
            errorMessages.add("basicParameters.terminationDate-termination date should be empty!;");
        }

        List<ServiceContractTermShortResponse> serviceContractTerms = sourceView.getServiceContractTerms();
        Map<Long, ServiceContractTermShortResponse> collect = serviceContractTerms.stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        ServiceContractTermShortResponse termsResponse = collect.get(serviceParameters.getContractTermId());
        if (termsResponse == null) {
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            return;
        }


        if (status.equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY) && perpetuityDate == null) {
            errorMessages.add("basicParameters.perpetuityDate-perpetuity date should not be empty!;");
        } else if (!status.equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY) && perpetuityDate != null) {
            errorMessages.add("basicParameters.perpetuityDate-perpetuity date should be empty!;");
        }
        if (perpetuityDate != null && perpetuityDate.isAfter(LocalDate.now())) {
            errorMessages.add("basicParameters.perpetuityDate-Perpetuity date should be in past or today!;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        contract.setPerpetuityDate(perpetuityDate);
        contract.setTerminationDate(terminationDate);
    }

    private LocalDate calculateTermEndDate(ServiceContractTermShortResponse termsResponse, LocalDate initialStartTerm, LocalDate endDateRequest) {
        if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD) && initialStartTerm != null) {

            LocalDate termEndDate = initialStartTerm.plus(termsResponse.getValue(), termsResponse.getTermType().getUnit()).minusDays(1);
            LocalDate maximumDate = LocalDate.parse(maxDate);
            return termEndDate.isBefore(maximumDate) ? termEndDate : maximumDate;

        } else if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
            return endDateRequest;
        } else if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.WITHOUT_TERM)) {
            return LocalDate.parse(maxDate);
        }
        return null;
    }

    private static boolean isInvalidSignedStatus(ServiceContractDetailStatus status, ServiceContractDetailsSubStatus subStatus) {
        return status.equals(ServiceContractDetailStatus.SIGNED) && List.of(ServiceContractDetailsSubStatus.SIGNED_BY_CUSTOMER, ServiceContractDetailsSubStatus.SIGNED_BY_EPRES).contains(subStatus);
    }

    private void throwExceptionIfDatesNotNull(LocalDate signingDate,
                                              LocalDate entryInForceDateFirst,
                                              LocalDate startOfInitialTerm,
                                              List<String> errorMessages) {
        if (signingDate != null) {
            errorMessages.add("basicParameters.signInDate-Signing date should be empty!;");
        }
        if (entryInForceDateFirst != null) {
            errorMessages.add("basicParameters.entryIntoForceDate-Entry in force date should be empty!;");
        }
        if (startOfInitialTerm != null) {
            errorMessages.add("basicParameters.startOfContractInitialTermDate-Start of initial term should be null!;");
        }
    }

    public void updateStatusesBasedOnDates(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails) {
        if(serviceContract.getContractStatus().equals(ServiceContractDetailStatus.ENTERED_INTO_FORCE) && !serviceContract.getEntryIntoForceDate().isAfter(LocalDate.now())){
            serviceContract.setContractStatus(ServiceContractDetailStatus.ACTIVE_IN_TERM);
            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.DELIVERY);
        }
    }
}
