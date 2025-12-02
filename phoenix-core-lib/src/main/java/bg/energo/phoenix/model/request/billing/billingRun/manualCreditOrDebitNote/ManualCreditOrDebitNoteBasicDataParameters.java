package bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.DocumentType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManualCreditOrDebitNoteBasicDataParameters {

    @NotNull(message = "manualCreditOrDebitNoteBasicDataParameters.basisForIssuing-[basisForIssuing] should not be null;")
    @Size(min = 1, max = 1024, message = "manualCreditOrDebitNoteBasicDataParameters.basisForIssuing-[basisForIssuing] size should be between 1 and 1024;")
    private String basisForIssuing;

    @Size(min = 1,max = 32,message = "manualCreditOrDebitNoteBasicDataParameters.numberOfIncomeAccount-[numberOfIncomeAccount] size should be between 1 and 32;")
    private String numberOfIncomeAccount;

    private boolean numberOfIncomeAccountManual;

    @Size(min = 1,max = 32,message = "manualCreditOrDebitNoteBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] min size is 1 and max is 32;")
    private String costCenterControllingOrder;

    private boolean costCenterControllingOrderManual;

    private Long vatRateId;

    private boolean globalVatRate;

    private boolean vatRateManual;

    private Long applicableInterestRateId;

    private boolean applicableInterestRateManual;

    private boolean directDebit;

    private boolean directDebitManual;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "manualCreditOrDebitNoteBasicDataParameters.iban")
    private String iban;

    @NotNull(message = "manualCreditOrDebitNoteBasicDataParameters.documentType-[documentType] should not be null;")
    private DocumentType documentType;

    //subobject

    @NotEmpty(message = "manualCreditOrDebitNoteBasicDataParameters.billingRunInvoiceInformationList-[billingRunInvoiceInformationList] billing run invoice information list should not be empty;")
    private List<BillingRunInvoiceInformation> billingRunInvoiceInformationList;

}
