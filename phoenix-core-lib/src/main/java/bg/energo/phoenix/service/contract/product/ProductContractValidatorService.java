package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.model.entity.product.product.ProductAdditionalParams;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DateOfIssueType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import bg.energo.phoenix.model.request.contract.product.ContractInterimAdvancePaymentsRequest;
import bg.energo.phoenix.model.request.contract.product.ContractProductAdditionalParamsRequest;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractThirdPageFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.product.ProductAdditionalParamsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductContractValidatorService {


    private final CurrencyRepository currencyRepository;
    private final ProductAdditionalParamsRepository productAdditionalParamsRepository;

    public void validateBaseCreateRequest(ProductParameterBaseRequest request, ProductContractThirdPageFields source, List<String> messages) {


        validateContractType(source, messages, request.getContractType());
        validateInvoicePaymentTerms(source, messages, request);
        validatePaymentGuarantee(source, messages, request);


//        validatePriceComponentValues(source, messages, request.getContractFormulas());
        validateInterimAdvancePayments(source, messages, request);
        validateWaitForOldContract(source, messages, request);

        validateProductAdditionalParameters(request.getProductAdditionalParams(), messages);
    }

    private void validateProductAdditionalParameters(List<ContractProductAdditionalParamsRequest> productAdditionalParams, List<String> messages) {
        int index = 0;
        for (ContractProductAdditionalParamsRequest params : productAdditionalParams) {
            Optional<ProductAdditionalParams> paramsOptional = productAdditionalParamsRepository.findById(params.getId());
            if (paramsOptional.isEmpty()) {
                messages.add("productParameters.productAdditionalParams[%s]-product additional params not found by id: %s".formatted(index, params.getId()));
            } else {
                if (paramsOptional.get().getValue() != null && !params.getValue().equals(paramsOptional.get().getValue())) {
                    messages.add("productParameters.productAdditionalParams[%s]-product additional params value is already filled and should not be changed;".formatted(index));
                }
            }
            index++;
        }
    }

    private void validateWaitForOldContract(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        WaitForOldContractTermToExpire waitForOldContract = request.getProductContractWaitForOldContractTermToExpires();
        List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires = source.getWaitForOldContractTermToExpires();
        if (CollectionUtils.isEmpty(waitForOldContractTermToExpires) && waitForOldContract != null) {
            messages.add("productParameters.productContractWaitForOldContractTermToExpires-Wait for old contract should not be provided!;");
            return;
        }
        if (waitForOldContract == null) {
            return;
        }
        if (!waitForOldContractTermToExpires.contains(waitForOldContract)) {
            messages.add("productParameters.productContractWaitForOldContractTermToExpires-Wait for old contract should not be provided!;");
            return;
        }
    }


    private void validateInterimAdvancePayments(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        Map<Long, InterimAdvancePaymentResponse> collected = source.getInterimAdvancePayments()
                .stream()
                .collect(Collectors.toMap(InterimAdvancePaymentResponse::getId, j -> j));
        List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments = request.getInterimAdvancePayments();
        //Todo add group checks
        int index = 0;
        for (ContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            InterimAdvancePaymentResponse paymentResponse = collected.get(interimAdvancePayment.getInterimAdvancePaymentId());
            if (!paymentResponse.isFromGroup()) {
                if (!collected.containsKey(interimAdvancePayment.getInterimAdvancePaymentId())) {
                    messages.add(String.format("productParameters.InterimAdvancePayments[%s].formulaVariableId-wrong interim advance payment selected;", index));
                    index++;
                    continue;
                }
                InterimAdvancePaymentTermsResponse paymentTerm = paymentResponse.getInterimAdvancePaymentTerm();
                CalendarType calendarType = paymentTerm.getCalendarType();
                if (calendarType != null && calendarType.equals(CalendarType.CERTAIN_DAYS) && interimAdvancePayment.getTermValue() == null) {
                    messages.add(String.format("productParameters.InterimAdvancePayments[%s].termValue-wrong interim advance payment value selected;", index));
                }
                validateIapFormulaVariables(paymentResponse, interimAdvancePayment, messages, index);
            }
            validateIssueDate(interimAdvancePayment.getIssueDate(), paymentResponse, messages, index);
            validateInterimValueType(interimAdvancePayment, paymentResponse, messages, index);
            index++;
        }
    }

    private void validateInterimValueType(ContractInterimAdvancePaymentsRequest interimAdvancePayment, InterimAdvancePaymentResponse paymentResponse, List<String> messages, int index) {
        if (ValueType.EXACT_AMOUNT.equals(paymentResponse.getValueType())) {
            if (interimAdvancePayment.getValue() == null) {
                messages.add("productParameters.InterimAdvancePayments[%s].value-value can not be null!;".formatted(index));
                return;
            }
            if (paymentResponse.getValue() != null && paymentResponse.getValue().compareTo(interimAdvancePayment.getValue()) != 0) {
                messages.add("productParameters.InterimAdvancePayments[%s].value-Value should equal to value provided in interim advance payment;".formatted(index));
            } else if (paymentResponse.getValueFrom() != null && paymentResponse.getValueTo() != null && (interimAdvancePayment.getValue().compareTo(paymentResponse.getValueFrom()) < 0 || interimAdvancePayment.getValue().compareTo(paymentResponse.getValueTo()) > 0)) {
                messages.add("productParameters.InterimAdvancePayments[%s].value-Value should be between %s and %s".formatted(index, paymentResponse.getValueFrom(), paymentResponse.getValueTo()));
            }
        }
    }

    private void validateIssueDate(Integer dateOfIssueValue, InterimAdvancePaymentResponse paymentResponse, List<String> messages, int index) {
        if (dateOfIssueValue != null) {
            DateOfIssueType dateOfIssueType = paymentResponse.getDateOfIssueType();
            int rangeMax = Objects.equals(dateOfIssueType, DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE) ? 9999 : 31;

            int rangeMin = 1;
            if (paymentResponse.getDateOfIssueValueFrom() != null) {
                rangeMin = paymentResponse.getDateOfIssueValueFrom();
            }
            if (paymentResponse.getDateOfIssueValueTo() != null) {
                rangeMax = paymentResponse.getDateOfIssueValueTo();
            }
            if (dateOfIssueValue < rangeMin || dateOfIssueValue > rangeMax) {
                messages.add(String.format("productParameters.InterimAdvancePayments[%s].issueDate-Issue Date must be between %s_%s;", index, rangeMin, rangeMax));
            }
        }
    }

    private void validateIapFormulaVariables(InterimAdvancePaymentResponse source, ContractInterimAdvancePaymentsRequest request, List<String> messages, int interimIndex) {
        PriceComponentFormula formula = source.getFormula();
        int index = 0;
        List<@Valid PriceComponentContractFormula> contractFormulas = request.getContractFormulas();
        if ((formula == null || formula.getPriceComponentId() == null) && !CollectionUtils.isEmpty(contractFormulas)) {
            messages.add("productParameters.InterimAdvancePayments[%s].contractFormulas - formulas should be empty!;"
                    .formatted(interimIndex));
            return;
        }
        if (formula != null && formula.getPriceComponentId() != null && !CollectionUtils.isEmpty(formula.getVariables()) && CollectionUtils.isEmpty(contractFormulas)) {
            messages.add("productParameters.InterimAdvancePayments[%s].contractFormulas - formulas should not be empty!;"
                    .formatted(interimIndex));
            return;
        }
        if (formula == null || CollectionUtils.isEmpty(formula.getVariables())) {
            return;
        }
        Map<Long, PriceComponentFormulaVariables> collect = formula.getVariables().stream().collect(Collectors.toMap(PriceComponentFormulaVariables::getFormulaVariableId, j -> j));
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            PriceComponentFormulaVariables remove = collect.remove(contractFormula.getFormulaVariableId());
            if (remove == null) {
                messages.add("productParameters.InterimAdvancePayments[%s].contractFormulas[%s].formulaVariableId - wrong formula id!;"
                        .formatted(interimIndex, index));
            }
        }
        Collection<PriceComponentFormulaVariables> values = collect.values();
        if (!CollectionUtils.isEmpty(values)) {
            messages.add("productParameters.InterimAdvancePayments[%s].contractFormulas - missing formula variables with ids: %s"
                    .formatted(interimIndex, values.stream().map(PriceComponentFormulaVariables::getFormulaVariableId).toList()));
        }
        index++;
    }

    public void validateEqualMonthlyInstallment(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        InstallmentForContractFields installmentForContractFields = source.getInstallmentForContractFields();

        Short monthlyInstallmentValue = request.getMonthlyInstallmentValue();
        BigDecimal monthlyInstallmentAmount = request.getMonthlyInstallmentAmount();
        if (Boolean.TRUE.equals(installmentForContractFields.getEqualMonthlyInstallmentsActivation())) {
            if (monthlyInstallmentValue == null) {
                messages.add("productParameters.monthlyInstallmentValue-Monthly installment value can not be null!;");
            } else if (installmentForContractFields.getInstallmentNumberTo() == null && installmentForContractFields.getInstallmentNumberFrom() == null && installmentForContractFields.getInstallmentNumber() != null) {
                if (!Objects.equals(monthlyInstallmentValue, installmentForContractFields.getInstallmentNumber())) {
                    messages.add("productParameters.monthlyInstallmentValue-Monthly installment value should equal to number provided in product!;");
                }
            } else {
                if ((installmentForContractFields.getInstallmentNumberFrom() != null && installmentForContractFields.getInstallmentNumberFrom() > monthlyInstallmentValue) || (installmentForContractFields.getInstallmentNumberTo() != null && installmentForContractFields.getInstallmentNumberTo() < monthlyInstallmentValue)) {
                    messages.add("productParameters.monthlyInstallmentValue-Should be between installment number from and installment number to!;");
                }
            }
            if (monthlyInstallmentAmount == null) {
                messages.add("productParameters.monthlyInstallmentAmount-Monthly installment amount can not be null!;");
            } else {
                BigDecimal amountTo = installmentForContractFields.getAmountTo();
                BigDecimal amountFrom = installmentForContractFields.getAmountFrom();
                if ((amountTo != null && amountTo.compareTo(monthlyInstallmentAmount) < 0)
                        || (amountFrom != null && amountFrom.compareTo(monthlyInstallmentAmount) > 0)) {
                    messages.add("productParameters.monthlyInstallmentAmount-Should be between installment amount from and installment amount to!;");
                }
                if (amountTo == null && amountFrom == null && installmentForContractFields.getAmount() != null) {
                    if (!Objects.equals(installmentForContractFields.getAmount(), monthlyInstallmentAmount)) {
                        messages.add("productParameters.monthlyInstallmentAmount-Monthly installment amount should equal to value provided in product!;");
                    }
                }
            }
            return;
        }
        if (monthlyInstallmentAmount != null || monthlyInstallmentValue != null) {
            messages.add("productParameters.monthlyInstallmentAmount-Monthly installment amount and value should be null!;");
        }
    }

    private void validateSupplyActivation(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        SupplyActivationsForContractFields supplyActivationsForContractFields = source.getSupplyActivationsForContractFields();
        List<SupplyActivation> supplyActivations = supplyActivationsForContractFields.getSupplyActivations();
        SupplyActivation supplyActivation = request.getSupplyActivation();
        if (CollectionUtils.isEmpty(supplyActivations) && supplyActivation == null) {
            return;
        } else if (CollectionUtils.isEmpty(supplyActivations)) {
            messages.add("productParameters.supplyActivation-should be null;");
            return;
        }
        if (!supplyActivations.contains(supplyActivation)) {
            messages.add("productParameters.supplyActivation-wrong value in supplyActivation;");
            return;
        }
        if (supplyActivation.equals(SupplyActivation.EXACT_DATE) && request.getSupplyActivationValue() == null) {
            messages.add("productParameters.supplyActivationValue-supplyActivationValue is not selected!;");
        } else if (supplyActivation.equals(SupplyActivation.EXACT_DATE)) {
            Integer value = supplyActivationsForContractFields.getSupplyActivationExactDateStartDay();
            if (value != null && request.getSupplyActivationValue().getDayOfMonth() != value) {
                messages.add("productParameters.supplyActivationValue-supplyActivationValue should equal to value provided in product term!;");
            }
        }
    }

    private void validateInitialTermStart(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        StartOfContractInitialTermsForContractFields contractEntryInitialTerm = source.getStartOfContractInitialTermsForContractFields();
        List<StartOfContractInitialTerm> initialTerms = contractEntryInitialTerm.getStartsOfContractInitialTerms();
        StartOfContractInitialTerm term = request.getStartOfContractInitialTerm();
        if (CollectionUtils.isEmpty(initialTerms) && term == null) {
            return;
        } else if (CollectionUtils.isEmpty(initialTerms)) {
            messages.add("productParameters.startOfContractInitialTerm-should be null;");
            return;
        }
        if (!initialTerms.contains(term)) {
            messages.add("productParameters.startOfContractInitialTerm-wrong value in startOfContractInitialTerm;");
            return;
        }
        if (term.equals(StartOfContractInitialTerm.EXACT_DATE) && request.getStartOfContractValue() == null) {
            messages.add("productParameters.startOfContractValue-startOfContractValue is not selected!;");
        } else if (term.equals(StartOfContractInitialTerm.EXACT_DATE)) {
            Integer value = contractEntryInitialTerm.getStartDayOfInitialContractTerm();
            if (value != null && request.getStartOfContractValue().getDayOfMonth() != value) {
                messages.add("productParameters.startOfContractValue-startOfContractValue should equal to value provided in product term!;");
            }
        }
    }

    private void validateForceEntry(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        ContractEntryIntoForceForContractFields contractEntryIntoForces = source.getContractEntryIntoForces();
        List<ContractEntryIntoForce> forces = contractEntryIntoForces.getContractEntryIntoForces();
        ContractEntryIntoForce entryIntoForce = request.getEntryIntoForce();
        if (CollectionUtils.isEmpty(forces) && entryIntoForce == null) {
            return;
        } else if (CollectionUtils.isEmpty(forces)) {
            messages.add("productParameters.entryIntoForce-should be null;");
            return;
        }
        if (!forces.contains(entryIntoForce)) {
            messages.add("productParameters.entryIntoForce-wrong value from entry into force;");
            return;
        }
        if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY) && request.getEntryIntoForceValue() == null) {

            messages.add("productParameters.entryIntoForceValue-entry into force value is not selected!;");
            return;
        } else if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            Integer value = contractEntryIntoForces.getContractEntryIntoForceFromExactDayOfMonthStartDay();
            if (value != null && request.getEntryIntoForceValue().getDayOfMonth() != value) {
                messages.add("productParameters.entryIntoForceValue-Entry into force day should equal to value provided in product term!;");
            }
        }
    }

    private void validatePriceComponentValues(ProductContractThirdPageFields source, List<String> messages, List<PriceComponentContractFormula> contractFormulas) {
        Map<Long, PriceComponentFormulaVariables> variables = source.getFormulaVariables()
                .stream()
                .filter(x -> !x.isFromGroup())
                .flatMap(x -> x.getVariables().stream())
                .collect(Collectors.toMap(PriceComponentFormulaVariables::getFormulaVariableId, j -> j));
        int index = 0;
        if (variables.size() != contractFormulas.size()) {
            messages.add("productParameters.contractFormulas-Invalid formulas provided");
            return;
        }
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            if (!variables.containsKey(contractFormula.getFormulaVariableId())) {
                messages.add(String.format("productParameters.contractFormulas[%s].formulaVariableId-You can not have this formula on this contract! ", index));
                continue;
            }
            PriceComponentFormulaVariables variable = variables.get(contractFormula.getFormulaVariableId());
            if (variable.getValueFrom() != null && contractFormula.getValue().compareTo(variable.getValueFrom()) < 0) {
                messages.add(String.format("productParameters.contractFormulas[%s].value-Value should be greater than value from provided in price component; ", index));
                continue;
            }
            if (variable.getValueTo() != null && contractFormula.getValue().compareTo(variable.getValueTo()) > 0) {
                messages.add(String.format("productParameters.contractFormulas[%s].value- Value should be less than value to provided in price component;", index));
            }
            index++;

        }

    }

    private void validatePaymentGuarantee(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        PaymentGuarantee paymentGuarantee = request.getPaymentGuarantee();
        List<PaymentGuarantee> paymentGuarantees = source.getPaymentGuarantees();
        if (CollectionUtils.isEmpty(paymentGuarantees) && paymentGuarantee == null) {
            return;
        } else if (CollectionUtils.isEmpty(paymentGuarantees)) {
            messages.add("productParameters.paymentGuarantees-Payment guarantee should be null!;");
            return;
        }
        if (!paymentGuarantees.contains(paymentGuarantee)) {
            messages.add("productParameters.paymentGuarantees-Wrong payment guarantee selected!;");
            return;
        }
        if ((paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK))) {
            if (request.getCashDeposit() == null) {
                messages.add("productParameters.cashDeposit-Cash deposit can not be null;");
            }
            if (request.getCashDepositCurrencyId() == null) {
                messages.add("productParameters.cashDepositCurrencyId-Cash deposit id can not be null;");
                return;
            }
            if (!currencyRepository.existsByIdAndStatusIn(request.getCashDepositCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))) {
                messages.add("productParameters.cashDepositCurrencyId-cash guarantee currency do not exist!;");
            }
        }
        if ((paymentGuarantee.equals(PaymentGuarantee.BANK) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK))) {
            if (request.getBankGuarantee() == null) {
                messages.add("productParameters.bankGuarantee-bank guarantee can not be null;");
            }
            if (request.getBankGuaranteeCurrencyId() == null) {
                messages.add("productParameters.bankGuaranteeCurrencyId-bank guarantee id can not be null;");
                return;
            }
            if (!currencyRepository.existsByIdAndStatusIn(request.getBankGuaranteeCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))) {
                messages.add("productParameters.bankGuaranteeCurrencyId-bank guarantee currency do not exist!;");
            }

        }
    }

    private void validateInvoicePaymentTerms(ProductContractThirdPageFields source, List<String> messages, ProductParameterBaseRequest request) {
        Map<Long, InvoicePaymentTermsResponse> invoicePaymentTerms = source.getInvoicePaymentTerms().stream().collect(Collectors.toMap(InvoicePaymentTermsResponse::getId, j -> j));
        InvoicePaymentTermsResponse termsResponse = invoicePaymentTerms.get(request.getInvoicePaymentTermId());
        if (termsResponse == null) {
            messages.add("productParameters.invoicePaymentTermId-wrong invoice payment term selected!;");
            return;
        }
        CalendarType calendarType = termsResponse.getCalendarType();
        if (calendarType.equals(CalendarType.CERTAIN_DAYS)) {
            Integer value = request.getInvoicePaymentTermValue();
            if (value == null) {
                messages.add("productParameters.invoicePaymentTermId-invoice payment term value can not be null!;");
                return;
            }

            Integer valueFrom = termsResponse.getValueFrom();
            Integer valueTo = termsResponse.getValueTo();

            if (valueFrom != null || valueTo != null) {
                if (valueFrom != null && value < valueFrom) {
                    messages.add("productParameters.invoicePaymentTermId-wrong invoice payment term value!;");
                }
                if (valueTo != null && value > valueTo) {
                    messages.add("productParameters.invoicePaymentTermId-wrong invoice payment term value!;");
                }
            } else {
                if (termsResponse.getValue() != null) {
                    if (!Objects.equals(value, termsResponse.getValue())) {
                        messages.add("productParameters.invoicePaymentTermId-wrong invoice payment term value!;");
                    }
                    return;
                }
            }

        }
    }


    private void validateContractType(ProductContractThirdPageFields source, List<String> messages, ContractType contractType) {
        List<ContractType> contractTypes = source.getContractTypes();
        if (CollectionUtils.isEmpty(contractTypes) && contractType == null) {
            return;
        } else if (CollectionUtils.isEmpty(contractTypes)) {
            messages.add("productParameters.contractType-should be null;");
            return;
        }
        if (!contractTypes.contains(contractType)) {
            messages.add("productParameters.contractType-wrong contract type selected!;");
        }

    }


}
