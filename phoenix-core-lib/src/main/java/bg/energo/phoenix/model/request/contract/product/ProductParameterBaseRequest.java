package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.products.AdditionalParametersValidator;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@AdditionalParametersValidator
public abstract class ProductParameterBaseRequest {
    @NotNull(message = "productParameters.contractType-contractType can not be null;")
    private ContractType contractType;

    @NotNull(message = "productParameters.productContractTermId-productContractTermId can not be null;")
    private Long productContractTermId;
    private LocalDate contractTermEndDate;

    //start of product guarantees
    @NotNull(message = "productParameters.paymentGuarantee-paymentGuarantee can not be null;")
    private PaymentGuarantee paymentGuarantee;
    @DecimalMin(value = "0.01", message = "productParameters.cashDeposit-cashDeposit can not be less than 0.01;")
    @DecimalMax(value = "99999999.99", message = "productParameters.bankGuarantee-bankGuarantee can not be greater than 99999999.99;")
    private BigDecimal cashDeposit;
    private Long cashDepositCurrencyId;
    @DecimalMin(value = "0.01", message = "productParameters.bankGuarantee-bankGuarantee can not be less than 0.01;")
    @DecimalMax(value = "99999999.99", message = "productParameters.bankGuarantee-bankGuarantee can not be greater than 99999999.99;")
    private BigDecimal bankGuarantee;
    private Long bankGuaranteeCurrencyId;
    private String guaranteeInformation;
    private boolean guaranteeContract;
    //end of product guarantees

    // product price component values
    @NotNull(message = "productParameters.contractFormulas-contractFormulas can not be null;")
    private List<@Valid PriceComponentContractFormula> contractFormulas = new ArrayList<>();

    //start of product term
    @NotNull(message = "productParameters.invoicePaymentTermId-invoicePaymentTermId can not be null;")
    private Long invoicePaymentTermId;
    private Integer invoicePaymentTermValue;

    @NotNull(message = "productParameters.entryIntoForce-entryIntoForce can not be null;")
    private ContractEntryIntoForce entryIntoForce;
    private LocalDate entryIntoForceValue;
    @NotNull(message = "productParameters.startOfContractInitialTerm-startOfContractInitialTerm can not be null;")
    private StartOfContractInitialTerm startOfContractInitialTerm;
    private LocalDate startOfContractValue;
    @NotNull(message = "productParameters.supplyActivation-supplyActivation can not be null;")
    private SupplyActivation supplyActivation;
    private LocalDate supplyActivationValue;
    //end of product term
    //product instalment vlaues
    private Short monthlyInstallmentValue;
    @DecimalMin(value = "1", message = "productParameters.monthlyInstallmentAmount-monthlyInstallmentAmount can not be less than 1;")
    @DecimalMax(value = "9999.99", message = "productParameters.monthlyInstallmentAmount-monthlyInstallmentAmount can not be greater than 9999.99;")
    private BigDecimal monthlyInstallmentAmount;
    public WaitForOldContractTermToExpire productContractWaitForOldContractTermToExpires;
    // product iaps values
    @NotNull(message = "productParameters.interimAdvancePayments-interimAdvancePayments can not be null;")
    private List<@Valid ContractInterimAdvancePaymentsRequest> interimAdvancePayments = new ArrayList<>();
    private List<@Valid ContractProductAdditionalParamsRequest> productAdditionalParams = new ArrayList<>();
}
