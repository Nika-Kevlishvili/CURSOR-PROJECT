package bg.energo.phoenix.model.response.contract.biling;


import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.enums.contract.billing.BillingGroupSendingInvoice;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import lombok.Data;

@Data
public class BillingGroupResponse {


    private Long id;
    private String groupNumber;
    private BillingGroupSendingInvoice sendingInvoice;
    private Boolean separateInvoiceForEachPod;
    private Boolean directDebit;
    private String iban;
    private String alternativeCustomerName;
    private Long alternativeRecipientCustomerDetailId;
    private Long customerId;
    private Long customerVersionId;

    private Long contractId;
    private BankResponse bankResponse;
    private String communicationName;
    private Long communicationId;

    public BillingGroupResponse(ContractBillingGroup billingGroup) {
        this.id = billingGroup.getId();
        this.groupNumber = billingGroup.getGroupNumber();
        this.sendingInvoice = billingGroup.getSendingInvoice();
        this.separateInvoiceForEachPod = billingGroup.getSeparateInvoiceForEachPod();
        this.directDebit = billingGroup.getDirectDebit();
        this.iban = billingGroup.getIban();
        this.alternativeRecipientCustomerDetailId = billingGroup.getAlternativeRecipientCustomerDetailId();
        this.contractId = billingGroup.getContractId();
        this.communicationId=billingGroup.getBillingCustomerCommunicationId();
    }
}
