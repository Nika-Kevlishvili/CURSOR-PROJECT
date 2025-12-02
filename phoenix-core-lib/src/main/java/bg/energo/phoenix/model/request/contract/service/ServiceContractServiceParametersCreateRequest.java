package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.customAnotations.contract.service.ContractServiceAdditionalParamsValidator;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractServiceParametersValidator;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ServiceContractServiceParametersValidator
@ContractServiceAdditionalParamsValidator
public class ServiceContractServiceParametersCreateRequest {

    @NotNull(message = "serviceParameters.contractTermId-[contractTermId] mustn't be null;")
    private Long contractTermId;

    private LocalDate contractTermEndDate;

    /*@NotNull(message = "serviceParameters.contractTermType-[contractTermType] mustn't be null;")
    private ServiceContractContractTermType contractTermType; //TODO these should be added in the database too*/

    //@NotNull(message = "serviceParameters.entryIntoForce-[entryIntoForce] mustn't be null;")
    private ContractEntryIntoForce entryIntoForce;

    private LocalDate entryIntoForceDate;

    private StartOfContractInitialTerm startOfContractInitialTerm;

    private LocalDate startOfContractInitialTermDate;

    @NotNull(message = "serviceParameters.invoicePaymentTermId-invoicePaymentTermId can not be null;")
    private Long invoicePaymentTermId;

    @NotNull(message = "serviceParameters.invoicePaymentTerm-[invoicePaymentTerm] mustn't be null;")
    private Integer invoicePaymentTerm;

    @NotNull(message = "serviceParameters.paymentGuarantee-[paymentGuarantee] mustn't be null;")
    private PaymentGuarantee paymentGuarantee;

    @DecimalMin(value = "0.01", message = "serviceParameters.cashDepositAmount-[cashDepositAmount] can not be less than 0.01;")
    @DecimalMax(value = "99999999.99", message = "serviceParameters.cashDepositAmount-[cashDepositAmount] can not be greater than 99999999.99;")
    private BigDecimal cashDepositAmount;

    private Long cashDepositCurrencyId;

    @DecimalMin(value = "0.01", message = "serviceParameters.bankGuaranteeAmount-[bankGuaranteeAmount] can not be less than 0.01;")
    @DecimalMax(value = "99999999.99", message = "serviceParameters.bankGuaranteeAmount-[bankGuaranteeAmount] can not be greater than 99999999.99;")
    private BigDecimal bankGuaranteeAmount;

    private Long bankGuaranteeCurrencyId;

    //private Long currency;

    //@NotNull(message = "serviceParameters.guaranteeContract-[guaranteeContract] mustn't be null;")
    private boolean guaranteeContract;

    private String guaranteeContractInfo;

    @NotNull(message = "serviceParameters.contractFormulas-contractFormulas can not be null;")
    private List<@Valid PriceComponentContractFormula> contractFormulas = new ArrayList<>();

    @Min(value = 1, message = "serviceParameters.quantity-[quantity] can not be less than 1;")
    @Max(value = 9999, message = "serviceParameters.quantity-[quantity] can not be greater than 9999;")
    private BigDecimal quantity;

    private List<@Valid ServiceContractInterimAdvancePaymentsRequest> interimAdvancePaymentsRequests = new ArrayList<>();

    private List<@Valid ContractServiceAdditionalParamsRequest> contractServiceAdditionalParamsRequests = new ArrayList<>();

    private Short monthlyInstallmentNumber;

    private BigDecimal monthlyInstallmentAmount;

    private List<String> contractNumbers;

    private List<Long> podIds;

    private List<String> unrecognizedPods;

}
