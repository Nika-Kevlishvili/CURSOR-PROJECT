package bg.energo.phoenix.model.response.billing.billingRun;


import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.IssuingForTheMonthToCurrent;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualInterimAndAdvancePaymentParametersResponse {

    private BigDecimal amountExcludingVat;

    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;

    private List<IssuedSeparateInvoice> issuedSeparateInvoices;

    private ShortResponse currencyResponse;

    private DeductionFrom deductionFrom;

    private String numberOfIncomeAccount;

    private boolean numberOfIncomeAccountManual;

    private String costCenterControllingOrder;

    private boolean costCenterControllingOrderManual;

    private VatRateResponse vatRate;

    private Boolean globalVatRate;

    private boolean vatRateManual;

    private InterestRateShortResponse applicableInterestRate;

    private boolean applicableInterestRateManual;

    private Boolean directDebit;

    private boolean directDebitManual;

    private BankResponse bank;

    @ValidIBAN(errorMessageKey = "manualInterimAndAdvancePaymentParametersResponse.iban")
    private String iban;

    private String basisForIssuing;

    private PrefixType prefixType;

    // sub objects

    private CustomerDetailsShortResponse customerDetails;

    private ContractType contractType;

    private ContractOrderShortResponse contract;

    private Set<BillingGroupListingResponse> billingGroupResponses;
}
