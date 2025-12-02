package bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.DocumentType;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCreditOrDebitNoteBasicDataParametersResponse {

    private String basisForIssuing;

    private String numberOfIncomeAccount;

    private boolean numberOfIncomeAccountManual;

    private String costCenterControllingOrder;

    private boolean costCenterControllingOrderManual;

    private VatRateResponse vatRate;

    private Boolean globalVatRate;

    private boolean vatRateManual;

    private InterestRateShortResponse applicableInterestRate;

    private Boolean applicableInterestRateManual;

    private Boolean directDebit;

    private Boolean directDebitManual;

    private BankResponse bank;

    @ValidIBAN(errorMessageKey = "manualCreditOrDebitNoteBasicDataParameterResponse.iban")
    private String iban;

    private DocumentType documentType;

    private List<BillingRunInvoiceResponse> invoiceResponseList;

}
