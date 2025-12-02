package bg.energo.phoenix.model.response.customer.customerRelated.relationship;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRelatedRelationshipResponse {
    private Long massOrIndCommunicationId;
    private Long linkedCommunicationId;
    private String activity;
    private String creatorEmployee;
    private String senderEmployee;
    private String contactPurpose;
    private String communicationType;
    private String communicationData;
    private LocalDateTime sentReceiveDate;
    private LocalDateTime createDate;
    private String communicationTopic;
    private String communicationStatus;
    private String status;
    private String communicationChannel;
    private String relationType;
    private String subject;

    public CustomerRelatedRelationshipResponse(CustomerRelatedRelationshipMiddleResponse middleResponse) {
        this.massOrIndCommunicationId = middleResponse.getMassOrIndCommunicationId();
        this.linkedCommunicationId = middleResponse.getLinkedCommunicationId();
        this.activity = middleResponse.getActivity();
        this.creatorEmployee = middleResponse.getCreatorEmployee();
        this.senderEmployee = middleResponse.getSenderEmployee();
        this.contactPurpose = middleResponse.getContactPurpose();
        this.communicationType = middleResponse.getCommunicationType();
        this.communicationData = middleResponse.getCommunicationData();
        this.sentReceiveDate = middleResponse.getSentReceiveDate();
        this.createDate = middleResponse.getCreateDate();
        this.communicationTopic = middleResponse.getCommunicationTopic();
        this.communicationStatus = middleResponse.getCommunicationStatus();
        this.status = middleResponse.getStatus();
        this.communicationChannel = middleResponse.getCommunicationChannel();
        this.relationType = middleResponse.getRelationType();
        this.subject = middleResponse.getSubject();
    }
}
