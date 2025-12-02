package bg.energo.phoenix.model.response.customer.communicationData;

public interface CustomerEmailCommDataMiddleResponse {
    Long getCommunicationId();

    Long getContactId();

    String getContactValue();

    Long getContactPurposeId();
}
