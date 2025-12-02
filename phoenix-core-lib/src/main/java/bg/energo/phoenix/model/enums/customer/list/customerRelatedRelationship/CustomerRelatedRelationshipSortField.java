package bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomerRelatedRelationshipSortField {
    MASS_OR_IND_COMMUNICATION_ID("massOrIndCommunicationId"),
    LINKED_COMMUNICATION_ID("LinkedCommunicationId"),
    ACTIVITY("activity"),
    CREATOR_EMPLOYEE("creatorEmployee"),
    SENDER_EMPLOYEE("senderEmployee"),
    CONTACT_PURPOSE("contactPurpose"),
    COMMUNICATION_TYPE("communicationType"),
    COMMUNICATION_DATA("communicationData"),
    SENT_RECEIVE_DATE("sentReceiveDate"),
    CREATE_DATE("createDate"),
    COMMUNICATION_TOPIC("communicationTopic"),
    COMMUNICATION_STATUS("communicationStatus"),
    STATUS("status"),
    COMMUNICATION_CHANNEL("communicationChannel"),
    RELATION_TYPE("relationType"),
    SUBJECT("subject");

    private final String value;
}
