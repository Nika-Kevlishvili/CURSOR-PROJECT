package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.response.contract.productContract.ContractPriceComponentResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceParametersPreview {

    private ServiceContractContractType contractType;
    private PaymentGuarantee paymentGuarantee;
    private BigDecimal cashDeposit;
    private CurrencyResponse cashDepositCurrency;
    private BigDecimal bankGuarantee;
    private CurrencyResponse bankDepositCurrency;
    private String guaranteeInformation;
    private boolean guaranteeContract;
    private ContractEntryIntoForce entryIntoForce;
    private LocalDate entryIntoForceValue;
    private StartOfContractInitialTerm startOfContractInitialTerm;
    private LocalDate startOfContractInitialTermDate;
    private Short monthlyInstallmentValue;
    private BigDecimal monthlyInstallmentAmount;
    private ServiceExecutionLevel executionLevel;

    private List<ContractPriceComponentResponse> priceComponents;
    private Integer quantity;
    private List<ServiceContractIAPResponse> interimAdvancePayments;
    private List<ServiceContractServiceAdditionalParamsResponse> serviceContractServiceAdditionalParams;
    private ServiceContractTermsResponse contractTerm;
    private InvoicePaymentTermsResponse invoicePaymentTerm;
    private Integer invoicePaymentTermValue;
    private LocalDate contractTermDate;

    private List<SubObjectContractResponse> contractResponseList;
    private List<SubObjectPodsResponse> podsList;
    private List<SubObjectPodsResponse> unrecognizablePodsList;
}
