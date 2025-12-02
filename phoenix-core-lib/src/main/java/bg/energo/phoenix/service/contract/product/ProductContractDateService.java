package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersUpdateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractThirdPageFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.ContractEntryIntoForceForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.StartOfContractInitialTermsForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.SupplyActivationsForContractFields;
import bg.energo.phoenix.model.response.product.ProductContractTermsResponse;
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
public class ProductContractDateService {
    @Value("${contract.without_term.value}")
    private String maxDate;

    //validates and sets dates
    public void validateDates(
            ProductContract contract,
            ProductContractDetails contractDetails,
            ProductContractBasicParametersCreateRequest basicParameters,
            ProductParameterBaseRequest productParameters,
            ProductContractThirdPageFields sourceView,
            List<String> errorMessages) {
        LocalDate signingDate = basicParameters.getSigningDate();
        LocalDate entryInForceDateFirst = basicParameters.getEntryInForceDate();
        LocalDate startOfInitialTerm = basicParameters.getStartOfInitialTerm();
        ContractDetailsStatus status = basicParameters.getStatus();
        ContractDetailsSubStatus subStatus = basicParameters.getSubStatus();
        if (status.equals(ContractDetailsStatus.DRAFT)) {
            throwExceptionIfDatesNotNull(signingDate, entryInForceDateFirst, startOfInitialTerm, errorMessages);
        } else if (status.equals(ContractDetailsStatus.READY)) {
            if (signingDate != null) {
                errorMessages.add("basicParameters.signingDate-Signing date should be empty!;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryInForceDate-Entry in force should be in future!;");
            }
        } else if (List.of(ContractDetailsStatus.ENTERED_INTO_FORCE, ContractDetailsStatus.ACTIVE_IN_PERPETUITY, ContractDetailsStatus.ACTIVE_IN_TERM, ContractDetailsStatus.TERMINATED)
                .contains(status)) {
            if (signingDate == null) {
                errorMessages.add("basicParameters.signingDate-Signing date is mandatory!;");
            } else if (signingDate.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.signingDate-Signing date should be today or in future;");
            }
            if (entryInForceDateFirst == null) {
                errorMessages.add("basicParameters.entryInForceDate-Entry in force is mandatory!;");
            } else {
                if (entryInForceDateFirst.isAfter(LocalDate.now())) {
                    errorMessages.add("basicParameters.entryInForceDate-Entry in force should be today or past!;");
                }
            }
        } else if (isInvalidSignedStatus(status, subStatus)) {
            if (signingDate != null) {
                errorMessages.add("basicParameters.signingDate-Signing date should be empty!;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryInForceDate-Entry in force should be in future!;");
            }
        } else if (status.equals(ContractDetailsStatus.SIGNED)) {
            if (signingDate == null) {
                errorMessages.add("basicParameters.signingDate-Signing date is mandatory!;");
            } else if (signingDate.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.signingDate-Signing date should be today or in future;");
            }
            if (entryInForceDateFirst != null && !entryInForceDateFirst.isAfter(LocalDate.now())) {
                errorMessages.add("basicParameters.entryInForceDate-Entry in force should be in future!;");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        contract.setSigningDate(signingDate);
        contract.setEntryIntoForceDate(entryInForceDateFirst);
        contract.setInitialTermDate(startOfInitialTerm);

        ContractEntryIntoForce entryIntoForce = productParameters.getEntryIntoForce();
        if (entryIntoForce.equals(ContractEntryIntoForce.SIGNING) && signingDate != null) {
            if (entryInForceDateFirst == null) {
                contract.setEntryIntoForceDate(signingDate);
            }
            contractDetails.setEntryIntoForceDate(signingDate);
        } else if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            if (entryInForceDateFirst == null && !status.equals(ContractDetailsStatus.DRAFT)) {

                contract.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
            }
            if (List.of(ContractDetailsStatus.READY, ContractDetailsStatus.SIGNED).contains(status)) {
                if (!productParameters.getEntryIntoForceValue().isAfter(LocalDate.now())) {
                    errorMessages.add("productParameters.entryInForceValue-Entry in force date should be in feature!;");
                }
            }
            contractDetails.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
        } else if ((entryIntoForce.equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) || entryIntoForce.equals(ContractEntryIntoForce.FIRST_DELIVERY)) && contract.getActivationDate() != null && contract.getEntryIntoForceDate() == null) {
            contract.setEntryIntoForceDate(contract.getActivationDate());
        }
        StartOfContractInitialTerm startOfContractInitialTerm = productParameters.getStartOfContractInitialTerm();
        if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.SIGNING) && signingDate != null) {
            if (startOfInitialTerm == null) {
                contract.setInitialTermDate(signingDate);
            }
            contractDetails.setInitialTermDate(signingDate);
        } else if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE)) {
            if (startOfInitialTerm == null && !status.equals(ContractDetailsStatus.DRAFT)) {
                contract.setInitialTermDate(productParameters.getStartOfContractValue());
            }
            contractDetails.setInitialTermDate(productParameters.getStartOfContractValue());
        } else if ((startOfContractInitialTerm.equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) || startOfContractInitialTerm.equals(StartOfContractInitialTerm.FIRST_DELIVERY)) && contract.getActivationDate() != null && contract.getInitialTermDate() == null) {
            contract.setInitialTermDate(contract.getActivationDate());
        } else if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.FIRST_DAY_MONTH_SIGNING) && signingDate != null) {
            int maxDay = YearMonth.now().lengthOfMonth();
            Integer contractDay = sourceView.getStartOfContractInitialTermsForContractFields().getFirstDayOfTheMonthOfInitialContractTerm();
            int day = contractDay > maxDay ? maxDay:contractDay;
            LocalDate calculatedDate = null;
            if (day >= signingDate.getDayOfMonth()) {
                LocalDate localDate = signingDate.plusMonths(1);

                calculatedDate=LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
            } else {
                LocalDate localDate = signingDate.plusMonths(2);
                calculatedDate=LocalDate.of(localDate.getYear(),localDate.getMonth(),1);
            }
            if(startOfInitialTerm==null){
                contract.setInitialTermDate(calculatedDate);
            }
            contractDetails.setInitialTermDate(calculatedDate);
        }
        contractDetails.setEntryIntoForce(entryIntoForce);
        contractDetails.setStartInitialTerm(startOfContractInitialTerm);
        contractDetails.setSupplyActivationAfterContractResigning(productParameters.getSupplyActivation());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

    }

    //validates product term values;
    public void validateSourceView(ProductContractThirdPageFields sourceView,
                                   ProductContractBasicParametersCreateRequest basicParameters,
                                   ProductParameterBaseRequest productParameters,
                                   List<String> errorMessages
    ) {
        ContractEntryIntoForceForContractFields contractEntryIntoForces = sourceView.getContractEntryIntoForces();
        if (!contractEntryIntoForces.getContractEntryIntoForces().contains(productParameters.getEntryIntoForce())) {
            errorMessages.add("productParameters.entryIntoForce-Wrong Entry into force provided!;");
        } else if (productParameters.getEntryIntoForce().equals(ContractEntryIntoForce.EXACT_DAY)
                   && contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay() != null
                   &&
//                   !Objects.equals(productParameters.getEntryIntoForceValue().getDayOfMonth(), contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay())) {
                   !validateLastDayOfTheMonth(productParameters.getEntryIntoForceValue(), contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay())) {
            errorMessages.add("productParameters.entryIntoForceValue-Wrong Entry into force value provided!;");
        }

        StartOfContractInitialTermsForContractFields initialTermFields = sourceView.getStartOfContractInitialTermsForContractFields();
        if (!initialTermFields.getStartsOfContractInitialTerms().contains(productParameters.getStartOfContractInitialTerm())) {
            errorMessages.add("productParameters.startOfContractInitialTerm-Wrong Start Of Contract Initial Term provided!;");
        } else if (productParameters.getStartOfContractInitialTerm().equals(StartOfContractInitialTerm.EXACT_DATE)
                   && initialTermFields.getStartDayOfInitialContractTerm() != null
//                   && !Objects.equals(productParameters.getStartOfContractValue().getDayOfMonth(), initialTermFields.getStartDayOfInitialContractTerm())) {
                   && !validateLastDayOfTheMonth(productParameters.getStartOfContractValue(), initialTermFields.getStartDayOfInitialContractTerm())) {
            errorMessages.add("productParameters.startOfContractInitialTerm-Wrong Start Of Contract Initial Term value provided!;");
        }

        SupplyActivationsForContractFields supplyFields = sourceView.getSupplyActivationsForContractFields();
        if (!supplyFields.getSupplyActivations().contains(productParameters.getSupplyActivation())) {
            errorMessages.add("productParameters.supplyActivation-Wrong Supply activation provided!;");
        } else if (productParameters.getSupplyActivation().equals(SupplyActivation.EXACT_DATE) && supplyFields.getSupplyActivationExactDateStartDay() != null &&
//                   !Objects.equals(productParameters.getSupplyActivationValue().getDayOfMonth(), supplyFields.getSupplyActivationExactDateStartDay())) {
                   !validateLastDayOfTheMonth(productParameters.getSupplyActivationValue(), supplyFields.getSupplyActivationExactDateStartDay())) {
            errorMessages.add("productParameters.supplyActivation-Wrong Supply activation value provided!;");
        }
    }

    private boolean validateLastDayOfTheMonth(LocalDate startOfTheContract, Integer startDayOfInitialContractTerm) {
        List<Integer> lastDays = List.of(31, 30, 29);
        int lastDayOfContractStartMonth = startOfTheContract.with(lastDayOfMonth()).getDayOfMonth();
        if (!Objects.equals(startOfTheContract.getDayOfMonth(), startDayOfInitialContractTerm)
            && lastDays.contains(startDayOfInitialContractTerm)
            && startOfTheContract.getDayOfMonth() == lastDayOfContractStartMonth) {
            return true;
        } else return Objects.equals(startOfTheContract.getDayOfMonth(), startDayOfInitialContractTerm);
    }

    //validates contract term and sets value in details
    public LocalDate validateContractTerm(ProductContract contract,
                                          ProductContractDetails contractDetails,
                                          ProductContractThirdPageFields sourceView,
                                          ProductContractBasicParametersCreateRequest basicParameters,
                                          ProductParameterBaseRequest productParameters,
                                          List<String> errorMessages) {
        List<ProductContractTermsResponse> productContractTerms = sourceView.getProductContractTerms();
        Map<Long, ProductContractTermsResponse> collect = productContractTerms.stream().collect(Collectors.toMap(ProductContractTermsResponse::getId, j -> j));
        if (!collect.containsKey(productParameters.getProductContractTermId())) {
            errorMessages.add("productParameters.productContractTermId-wrong contract term selected;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        ProductContractTermsResponse termsResponse = collect.get(productParameters.getProductContractTermId());
        if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.CERTAIN_DATE) && productParameters.getContractTermEndDate() == null) {
            errorMessages.add("productParameters.contractTermEndDate-should not be null;");
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        LocalDate termEndDate = calculateTermEndDate(termsResponse, contract.getInitialTermDate(), productParameters.getContractTermEndDate());
        contractDetails.setContractTermEndDate(termEndDate);

        return termEndDate;

    }

    //Used for update request
    public void setContractTermEndDate(ProductContract contract,
                                       ProductContractBasicParametersUpdateRequest basicParameters,
                                       LocalDate termEndDate) {
        if (!basicParameters.getStatus().equals(ContractDetailsStatus.DRAFT) && basicParameters.getContractTermEndDate() == null) {
            contract.setContractTermEndDate(termEndDate);
        } else if (basicParameters.getContractTermEndDate() != null) {
            contract.setContractTermEndDate(basicParameters.getContractTermEndDate());
        }
    }

    public void validatePerpetuity(ProductContract contract, ProductContractThirdPageFields sourceView,
                                   ProductContractBasicParametersUpdateRequest basicParameters,
                                   ProductParameterBaseRequest productParameters,
                                   List<String> errorMessages
    ) {
        ContractDetailsStatus status = basicParameters.getStatus();
        if (status.equals(ContractDetailsStatus.TERMINATED) && basicParameters.getTerminationDate() == null) {
            errorMessages.add("basicParameters.terminationDate-termination  date should not be empty!;");
        } else if (!status.equals(ContractDetailsStatus.TERMINATED) && basicParameters.getTerminationDate() != null) {
            errorMessages.add("basicParameters.terminationDate-termination date should be empty!;");
        }

        List<ProductContractTermsResponse> productContractTerms = sourceView.getProductContractTerms();
        Map<Long, ProductContractTermsResponse> collect = productContractTerms.stream().collect(Collectors.toMap(ProductContractTermsResponse::getId, j -> j));
        ProductContractTermsResponse termsResponse = collect.get(productParameters.getProductContractTermId());
        if (termsResponse == null) {
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            return;
        }


        if (status.equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY) && basicParameters.getPerpetuityDate() == null) {
            errorMessages.add("basicParameters.perpetuityDate-perpetuity date should not be empty!;");
        } else if (!status.equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY) && basicParameters.getPerpetuityDate() != null) {
            errorMessages.add("basicParameters.perpetuityDate-perpetuity date should be empty!;");
        }
        if (basicParameters.getPerpetuityDate() != null && basicParameters.getPerpetuityDate().isAfter(LocalDate.now())) {
            errorMessages.add("basicParameters.perpetuityDate-Perpetuity date should be in past or today!;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        contract.setPerpetuityDate(basicParameters.getPerpetuityDate());
        contract.setTerminationDate(basicParameters.getTerminationDate());
    }

    private LocalDate calculateTermEndDate(ProductContractTermsResponse termsResponse, LocalDate initialStartTerm, LocalDate endDateRequest) {
        if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.PERIOD) && initialStartTerm != null) {

            LocalDate termEndDate = initialStartTerm.plus(termsResponse.getValue(), termsResponse.getPeriodType().getUnit()).minusDays(1);
            LocalDate maximumDate = LocalDate.parse(maxDate);
            return termEndDate.isBefore(maximumDate) ? termEndDate : maximumDate;

        } else if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.CERTAIN_DATE)) {
            return endDateRequest;
        } else if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.WITHOUT_TERM)) {
            return LocalDate.parse(maxDate);
        }
        return null;
    }

    private static boolean isInvalidSignedStatus(ContractDetailsStatus status, ContractDetailsSubStatus subStatus) {
        return status.equals(ContractDetailsStatus.SIGNED) && List.of(ContractDetailsSubStatus.SIGNED_BY_CUSTOMER, ContractDetailsSubStatus.SIGNED_BY_EPRES).contains(subStatus);
    }

    private void throwExceptionIfDatesNotNull(LocalDate signingDate,
                                              LocalDate entryInForceDateFirst,
                                              LocalDate startOfInitialTerm,
                                              List<String> errorMessages) {
        if (signingDate != null) {
            errorMessages.add("basicParameters.signingDate-Signing date should be empty!;");
        }
        if (entryInForceDateFirst != null) {
            errorMessages.add("basicParameters.entryInForceDate-Entry in force date should be empty!;");
        }
        if (startOfInitialTerm != null) {
            errorMessages.add("basicParameters.startOfInitialTerm-Start of initial term should be null!;");
        }
    }
}
