package bg.energo.phoenix.model.response.contract;


public interface ContractDocumentEmailResponse {
    String getContractDocumentIds();

    String getDocumentUrls();

    String getDocumentNames();

    Long getCustomerCommunicationId();

    Long getCustomerDetailId();

    Long getMailboxId();

    String getEmails();

    Long getEmailTemplateId();

    Long getContractId();

    Long getContractVersion();

    String getEmailSubject();
}
