package bg.energo.phoenix.model.response.crm.emailCommunication;

public interface MassCommunicationFileProcessedResultProjection {
    String getCustomerIdentifier();
    Long getCustomerVersionId();
    Long getProductContractDetailId();
    Long getServiceContractDetailId();
}
