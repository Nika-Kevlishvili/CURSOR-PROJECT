package bg.energo.phoenix.model.response.crm.emailCommunication;


public record MassCommunicationFileProcessedResult(
        String customerIdentifier,
        Long customerVersionId,
        Long productContractDetailId,
        Long serviceContractDetailId
) {
    public MassCommunicationFileProcessedResult(MassCommunicationFileProcessedResultProjection projection) {
        this(
                projection.getCustomerIdentifier(),
                projection.getCustomerVersionId(),
                projection.getProductContractDetailId(),
                projection.getServiceContractDetailId()
        );
    }

}
