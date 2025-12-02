package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManualInvoiceBasicDataParametersResponse {

    private String basisForIssuing;

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

    @ValidIBAN(errorMessageKey = "manualInvoiceBasicDataParameterResponse.iban")
    private String iban;

    private PrefixType prefixType;

    private CustomerDetailsShortResponse customerDetails;

    private ContractOrderType contractOrderType;

    private ContractOrderShortResponse contractOrder;

    private BillingGroupListingResponse billingGroupResponse;

    private CustomerCommunicationDataResponse invoiceCommunicationData;

}
