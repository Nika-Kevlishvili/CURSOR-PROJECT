package bg.energo.phoenix.model.request.contract.billing;

import bg.energo.phoenix.model.customAnotations.contract.billing.BillingGroupRequestValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.contract.billing.BillingGroupSendingInvoice;
import lombok.Data;

@Data
@BillingGroupRequestValidator
public class BillingGroupRequest {

    private String groupNumber;
    private BillingGroupSendingInvoice sendingInvoice;
    private boolean separateInvoiceForEachPod;
    private boolean directDebit;
    private Long bankId;

    @ValidIBAN(errorMessageKey = "billingGroupRequest.iban")
    private String iban;

    private Long alternativeRecipientCustomerDetailId;
    private Long billingCustomerCommunicationId;
    private Long contractId;

}
