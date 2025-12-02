package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManualInvoiceBasicDataParameters {

    @NotNull(message = "manualInvoiceBasicDataParameters.basisForIssuing-[basisForIssuing] should not be null;")
    @Size(min = 1, max = 1024)
    private String basisForIssuing;

    @Size(min = 1,max = 32)
    private String numberOfIncomeAccount;

    private boolean numberOfIncomeAccountManual;

    @Size(min = 1,max = 32,message = "manualInvoiceBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] min size is 1 and max is 32;")
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

    @ValidIBAN(errorMessageKey = "manualInvoiceBasicDataParameters.iban")
    private String iban;

    private PrefixType prefixType;

    //subobjects

    @NotNull(message = "manualInvoiceBasicDataParameters.customerId-[customerId] customer id is mandatory;")
    private Long customerDetailId;

    private ContractOrderType contractOrderType;

    private Long contractOrderId;

    private Long billingGroupId;

    @NotNull(message = "manualInvoiceBasicDataParameters.invoiceCommunicationDataId-[invoiceCommunicationDataId] invoice communication data id is mandatory;")
    private Long invoiceCommunicationDataId;

}
