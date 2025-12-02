package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.product.ProductContractTermsResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ThirdPagePreview {

    private ContractType contractType;
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
    private LocalDate startOfContractValue;
    private SupplyActivation supplyActivation;
    private LocalDate supplyActivationValue;

    private Short monthlyInstallmentValue;
    private BigDecimal monthlyInstallmentAmount;

    private BigDecimal marginalPrice;
    private String marginalPriceValidity;
    private BigDecimal hourlyLoadProfile;
    private BigDecimal procurementPrice;
    private BigDecimal imbalancePriceIncrease;
    private BigDecimal setMargin;

    public WaitForOldContractTermToExpire productContractWaitForOldContractTermToExpires;

    private List<ContractPriceComponentResponse> priceComponents;
    private List<ProductContractIAPResponse> interimAdvancePayments;
    private List<ProductContractProductAdditionalParamsResponse> productContractProductAdditionalParamsResponseList;
    private ProductContractTermsResponse contractTerm;
    private LocalDate contractTermDate;
    private InvoicePaymentTermsResponse invoicePaymentTerm;
    private Integer invoicePaymentTermValue;

}
