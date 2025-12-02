package bg.energo.phoenix.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class CustomerEmailCommunicationPurposeKeyPair {

    private Long communicationId;
    private Long contactPurposeId;

    public CustomerEmailCommunicationPurposeKeyPair(CustomerEmailCommDataMiddleResponse middleResponse) {
        this.communicationId = middleResponse.getCommunicationId();
        this.contactPurposeId = middleResponse.getContactPurposeId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerEmailCommunicationPurposeKeyPair that = (CustomerEmailCommunicationPurposeKeyPair) o;
        return Objects.equals(communicationId, that.communicationId) &&
                Objects.equals(contactPurposeId, that.contactPurposeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(communicationId, contactPurposeId);
    }

    @Override
    public String toString() {
        return String.format("CommunicationId: %d, ContactPurposeId: %d", communicationId, contactPurposeId);
    }

}
