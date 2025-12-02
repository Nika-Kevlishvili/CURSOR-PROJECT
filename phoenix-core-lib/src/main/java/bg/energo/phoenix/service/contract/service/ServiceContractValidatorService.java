package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractInterimAdvancePaymentResponse;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractInvoicePaymentTermsResponse;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractPriceComponentFormulaVariables;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractThirdPageFields;
import bg.energo.phoenix.model.entity.product.service.ServiceAdditionalParams;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DateOfIssueType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractServiceParametersRequest;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.service.ContractServiceAdditionalParamsRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractInterimAdvancePaymentsRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.InstallmentForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.PriceComponentFormulaVariables;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceContractValidatorService {

    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;

    public void validateCreateRequest(LocalDate entryIntoForceDate, LocalDate signInDate, ServiceContractServiceParametersCreateRequest request, ServiceContractThirdPageFields source, List<String> errorMessages) {
        List<String> messages = new ArrayList<>();
        validateInvoicePaymentTerms(source, messages, request);
        validatePaymentGuarantee(source, messages, request);
        validatePriceComponentValues(source, messages, request.getContractFormulas());
        validateInterimAdvancePayments(source, messages, request);
        validateContractServiceAdditionalParams(messages, request.getContractServiceAdditionalParamsRequests());
        errorMessages.addAll(messages);
        //EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);
    }

    public void validateEditRequest(ServiceContractEditRequest serviceContractEditRequest, ServiceContractServiceParametersCreateRequest request, ServiceContractThirdPageFields source) {
        List<String> messages = new ArrayList<>();
        validateInvoicePaymentTerms(source, messages, request);
        validatePaymentGuarantee(source, messages, request);
        validatePriceComponentValues(source, messages, request.getContractFormulas());
        validatePriceComponentValuesContractFormulas(source, messages, request.getInterimAdvancePaymentsRequests(),request);
        validateInterimAdvancePayments(source, messages, request);
        validateContractServiceAdditionalParams(messages, request.getContractServiceAdditionalParamsRequests());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);
    }

    private void validateContractServiceAdditionalParams(List<String> messages, List<ContractServiceAdditionalParamsRequest> contractServiceAdditionalParamsRequests) {
        int index = 0;
        for (ContractServiceAdditionalParamsRequest params : contractServiceAdditionalParamsRequests) {
            Optional<ServiceAdditionalParams> paramsOptional = serviceAdditionalParamsRepository.findById(params.getId());
            if (paramsOptional.isEmpty()) {
                messages.add("serviceParameters.serviceAdditionalParams[%s]-service additional params not found by id: %s".formatted(index, params.getId()));
            } else {
                if (paramsOptional.get().getValue() != null && !params.getValue().equals(paramsOptional.get().getValue())) {
                    messages.add("serviceParameters.serviceAdditionalParams[%s]-service additional params value is already filled and should not be changed;".formatted(index));
                }
            }
            index++;
        }
    }

    private void validatePriceComponentValuesContractFormulas(ServiceContractThirdPageFields source, List<String> messages, List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePaymentsRequests, ServiceContractServiceParametersCreateRequest request) {
        if (!CollectionUtils.isEmpty(interimAdvancePaymentsRequests)) {
            for (ServiceContractInterimAdvancePaymentsRequest item : interimAdvancePaymentsRequests) {
                List<PriceComponentContractFormula> contractFormulas = item.getContractFormulas();
                //validatePriceComponentValues(source, messages, contractFormulas);
                validateInterimAdvancePaymentsFormulas(source, messages, request);
            }
        }

    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    private void validateInterimAdvancePaymentsFormulas(ServiceContractThirdPageFields source, List<String> messages, ServiceContractServiceParametersCreateRequest request) {
        Map<Long, List<PriceComponentFormulaVariables>> variables = source.getInterimAdvancePayments().stream()
                .filter(x->x.getValueType().equals(ValueType.PRICE_COMPONENT))
                .flatMap(interim ->{
                    if(interim.getFormula()!=null && interim.getFormula().getVariables()!=null){
                        return interim.getFormula().getVariables().stream();
                    }
                    return new ArrayList<PriceComponentFormulaVariables>().stream();
                })
                .collect(Collectors.groupingBy(PriceComponentFormulaVariables::getFormulaVariableId));
        if(!CollectionUtils.isEmpty(variables)){
            List<PriceComponentContractFormula> contractFormulas = request.getInterimAdvancePaymentsRequests().stream()
                    .flatMap(req -> req.getContractFormulas().stream()).toList();
            if(!CollectionUtils.isEmpty(contractFormulas)){
                Integer index = 0;
                for(PriceComponentContractFormula contractFormula : contractFormulas){
                    if(!variables.containsKey(contractFormula.getFormulaVariableId())){
                        messages.add(String.format("serviceParameters.interimAdvancePaymentsRequests[%s].formulaVariableId-You can not have this formula on this contract! ", index));
                        continue;
                    }
                    index++;
                }
            }
        }
    }


    private void validateInterimAdvancePayments(ServiceContractThirdPageFields source, List<String> messages, ServiceContractServiceParametersCreateRequest request) {
        Map<Long, ServiceContractInterimAdvancePaymentResponse> collected = source.getInterimAdvancePayments().stream().
                filter(x->!x.isFromGroup()).collect(Collectors.toMap(ServiceContractInterimAdvancePaymentResponse::getId, j -> j));
        List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePayments = request.getInterimAdvancePaymentsRequests();
        int index = 0;
        for (ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            if (!collected.containsKey(interimAdvancePayment.getInterimAdvancePaymentId())) {
                messages.add(String.format("serviceParameters.interimAdvancePayments[%s].interimAdvancePaymentsId-wrong interim advance payment selected;", index));
                index++;
                continue;
            }
            ServiceContractInterimAdvancePaymentResponse paymentResponse = collected.get(interimAdvancePayment.getInterimAdvancePaymentId());
            InterimAdvancePaymentTermsResponse paymentTerm = paymentResponse.getInterimAdvancePaymentTerm();
            if (paymentResponse.getValueType().equals(ValueType.PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT) || paymentResponse.getValueType().equals(ValueType.EXACT_AMOUNT)) {
                BigDecimal valueFrom = paymentResponse.getValueFrom();
                BigDecimal valueTo = paymentResponse.getValueTo();
                if (interimAdvancePayment.getValue() != null) {
                    BigDecimal value = interimAdvancePayment.getValue();

                    if (valueFrom != null && value.compareTo(valueFrom) < 0) {
                        messages.add("serviceParameters.interimAdvancePayments[%s].value-Value should be greater than %s".formatted(index, valueFrom));
                    }
                    if (valueTo != null && value.compareTo(valueTo) > 0) {
                        messages.add("serviceParameters.interimAdvancePayments[%s].value-Value should be less than %s".formatted(index, valueTo));
                    }
                }

            }
            validateIssueDate(interimAdvancePayment.getIssueDate(), paymentResponse, messages, index);
            if (paymentTerm != null) {
                if (paymentTerm.getCalendarType() != null) {
                    if (paymentTerm.getCalendarType().equals(CalendarType.CERTAIN_DAYS) && interimAdvancePayment.getTermValue() == null) {
                        messages.add(String.format("serviceParameters.PriceComponentContractFormula[%s].value-wrong interim advance payment value selected;", index));
                    }
                }
                Integer value = paymentTerm.getValue();
                Integer termValue = interimAdvancePayment.getTermValue();
                if(value!=null && !Objects.equals(termValue, value)){
                    messages.add("serviceParameters.interimAdvancePayments[%s].termValue-Term value should equal to %s".formatted(index, value));
                    return;
                }
                Integer valueFrom = paymentTerm.getValueFrom();
                Integer valueTo = paymentTerm.getValueTo();
                if(valueFrom!=null && termValue<valueFrom){
                    messages.add("serviceParameters.interimAdvancePayments[%s].termValue-Term value should be greater than %s".formatted(index, valueFrom));
                }
                if(valueTo !=null && termValue>valueTo){
                    messages.add("serviceParameters.interimAdvancePayments[%s].termValue-Term value should be less than %s".formatted(index, valueTo));
                }
            }
            index++;
        }
    }

    private void validateIssueDate(Integer dateOfIssueValue, ServiceContractInterimAdvancePaymentResponse paymentResponse, List<String> messages, int index) {
        if(dateOfIssueValue != null) {
            DateOfIssueType dateOfIssueType = paymentResponse.getDateOfIssueType();
            int rangeMax = dateOfIssueType.equals(DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE)? 9999 : 31;

            int rangeMin = 1;
            if(paymentResponse.getDateOfIssueValueFrom() != null) {
                rangeMin = paymentResponse.getDateOfIssueValueFrom();
            }
            if(paymentResponse.getDateOfIssueValueTo() != null) {
                rangeMax = paymentResponse.getDateOfIssueValueTo();
            }
            if(dateOfIssueValue < rangeMin || dateOfIssueValue > rangeMax) {
                messages.add(String.format("serviceParameters.InterimAdvancePayments[%s].issueDate-Issue Date must be between %s_%s;", index, rangeMin, rangeMax));
            }
        }
    }

    private void validatePriceComponentValues(ServiceContractThirdPageFields source, List<String> messages, List<PriceComponentContractFormula> contractFormulas) {
        Map<Long, ServiceContractPriceComponentFormulaVariables> variables = source.getFormulaVariables().stream().filter(x->!x.isFromGroup()).flatMap(x -> x.getVariables().stream()).collect(Collectors.toMap(ServiceContractPriceComponentFormulaVariables::getFormulaVariableId, j -> j));
        int index = 0;
        if (!CollectionUtils.isEmpty(contractFormulas)) {
            for (PriceComponentContractFormula contractFormula : contractFormulas) {
                if (!variables.containsKey(contractFormula.getFormulaVariableId())) {
                    messages.add(String.format("serviceParameters.contractFormulas[%s].formulaVariableId-You can not have this formula on this contract! ", index));
                    continue;
                }
                ServiceContractPriceComponentFormulaVariables variable = variables.get(contractFormula.getFormulaVariableId());
                if (variable.getValueFrom() != null && contractFormula.getValue().compareTo(variable.getValueFrom()) < 0) {
                    messages.add(String.format("serviceParameters.contractFormulas[%s].value-Value should be greater than value from provided in price component; ", index));
                    continue;
                }
                if (variable.getValueTo() != null && contractFormula.getValue().compareTo(variable.getValueTo()) > 0) {
                    messages.add(String.format("serviceParameters.contractFormulas[%s].value- Value should be less than value to provided in price component;", index));
                }
                index++;
            }
        }
    }

    private void validatePaymentGuarantee(ServiceContractThirdPageFields source, List<String> messages, ServiceContractServiceParametersCreateRequest request) {
        PaymentGuarantee paymentGuarantee = request.getPaymentGuarantee();
        List<PaymentGuarantee> paymentGuarantees = source.getPaymentGuarantees();
        if (CollectionUtils.isEmpty(paymentGuarantees) && paymentGuarantee == null) {
            return;
        } else if (CollectionUtils.isEmpty(paymentGuarantees)) {
            messages.add("serviceParameters.contractType-should be null;");
            return;
        }
        if (!paymentGuarantees.contains(paymentGuarantee)) {
            messages.add("serviceParameters.paymentGuarantee-wrong payment guarantee selected!;");
            return;
        }
        if ((paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK))) {
            if (request.getCashDepositAmount() == null) {
                messages.add("serviceParameters.cashDeposit-Cash deposit can not be null;");
            }
            if (request.getCashDepositCurrencyId() == null) {
                messages.add("serviceParameters.cashDepositCurrencyId-Cash deposit id can not be null;");
            }
        }
        if ((paymentGuarantee.equals(PaymentGuarantee.BANK) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK))) {
            if (request.getBankGuaranteeAmount() == null) {
                messages.add("serviceParameters.bankGuarantee-bank guarantee can not be null;");
            }
            if (request.getBankGuaranteeCurrencyId() == null) {
                messages.add("serviceParameters.bankGuaranteeCurrencyId-bank guarantee id can not be null;");
            }
        }
    }

    private void validateInvoicePaymentTerms(ServiceContractThirdPageFields source, List<String> messages, ServiceContractServiceParametersCreateRequest request) {
        Map<Long, ServiceContractInvoicePaymentTermsResponse> invoicePaymentTerms = source.getInvoicePaymentTerms().stream().collect(Collectors.toMap(x -> x.getId(), j -> j));
        if (!invoicePaymentTerms.containsKey(request.getInvoicePaymentTermId())) {
            messages.add("serviceParameters.invoicePaymentTerm-wrong invoice payment term selected!;");
        }
        ServiceContractInvoicePaymentTermsResponse termsResponse = invoicePaymentTerms.get(request.getInvoicePaymentTermId());
        if (termsResponse != null) {
            Integer value = request.getInvoicePaymentTerm();
            if (value == null) {
                messages.add("serviceParameters.invoicePaymentTerm-invoice payment term value can not be null!;");
                return;
            }
            Integer valueFrom = termsResponse.getValueFrom();
            Integer valueTo = termsResponse.getValueTo();
            if (termsResponse.getValue() != null && (valueFrom == null && valueTo == null)) {
                if (!Objects.equals(value, termsResponse.getValue())) {
                    messages.add("serviceParameters.invoicePaymentTerm-wrong invoice payment term value!;");
                }
                return;
            }
            if (valueFrom != null && valueTo != null) {
                if (!(value >= valueFrom && value <= valueTo)) {
                    messages.add("serviceParameters.invoicePaymentTerm-wrong invoice payment term value!;");
                }
            }
        } else {
            messages.add("serviceParameters.invoicePaymentTerm-wrong invoice payment term selected!;");
        }
    }


    public void validateMonthlyInstallment(ServiceContractThirdPageFields sourceView, List<String> messages, ExpressContractServiceParametersRequest serviceParametersRequest) {
        InstallmentForContractFields installmentForContractFields = sourceView.getInstallmentForContractFields();
        Short monthlyInstallmentValue = serviceParametersRequest.getMonthlyInstallmentNumber();
        BigDecimal monthlyInstallmentAmount = serviceParametersRequest.getMonthlyInstallmentAmount();
        if(Boolean.TRUE.equals(installmentForContractFields.getEqualMonthlyInstallmentsActivation())){
            if(monthlyInstallmentValue == null){
                messages.add("serviceParameters.monthlyInstallmentValue-Monthly installment value can not be null!;");
            }else {
                if((installmentForContractFields.getInstallmentNumberFrom()!=null&&installmentForContractFields.getInstallmentNumberFrom()>monthlyInstallmentValue) || (installmentForContractFields.getInstallmentNumberTo()!=null && installmentForContractFields.getInstallmentNumberTo()<monthlyInstallmentValue)){
                    messages.add("serviceParameters.monthlyInstallmentValue-Should be between installment number from and installment number to!;");
                }
            }
            if (monthlyInstallmentAmount == null) {
                messages.add("serviceParameters.monthlyInstallmentAmount-Monthly installment amount can not be null!;");
            }else {
                BigDecimal amountTo = installmentForContractFields.getAmountTo();
                BigDecimal amountFrom = installmentForContractFields.getAmountFrom();
                if((amountTo!=null&& amountTo.compareTo(monthlyInstallmentAmount)<0)
                        || (amountFrom!=null&&amountFrom.compareTo(monthlyInstallmentAmount)>0)){
                    messages.add("serviceParameters.monthlyInstallmentAmount-Should be between installment amount from and installment amount to!;");
                }
            }
            return;
        }
        if (monthlyInstallmentAmount != null || monthlyInstallmentValue != null) {
            messages.add("serviceParameters.monthlyInstallmentAmount-Monthly installment amount and value should be null!;");
        }
    }
}
