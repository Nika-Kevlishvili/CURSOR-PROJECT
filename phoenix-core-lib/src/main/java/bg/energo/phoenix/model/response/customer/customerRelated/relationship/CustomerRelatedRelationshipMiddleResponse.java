package bg.energo.phoenix.model.response.customer.customerRelated.relationship;

import java.time.LocalDateTime;

public interface CustomerRelatedRelationshipMiddleResponse {
    Long getMassOrIndCommunicationId();
    Long getLinkedCommunicationId();
    String getActivity();
    String getCreatorEmployee();
    String getSenderEmployee();
    String getContactPurpose();
    String getCommunicationType();
    String getCommunicationData();
    LocalDateTime getSentReceiveDate();
    LocalDateTime getCreateDate();
    String getCommunicationTopic();
    String getCommunicationStatus();
    String getStatus();
    String getCommunicationChannel();
    String getRelationType();
    String getSubject();
}
