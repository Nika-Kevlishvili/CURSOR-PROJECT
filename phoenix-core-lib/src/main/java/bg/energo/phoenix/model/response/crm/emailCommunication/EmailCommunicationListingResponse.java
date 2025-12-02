package bg.energo.phoenix.model.response.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailCommunicationListingResponse {

    private Long massOrIndemailCommunicationId;
    private Long linkedEmailCommunicationId;
    private String customerName;
    private String uicOrPersonalNumber;
    private String activity;
    private String creatorEmployee;
    private String senderEmployee;
    private String contactPurpose;
    private EmailCommunicationType communicationType;
    private String communicationData;
    private LocalDateTime sentReceiveDate;
    private LocalDateTime createDate;
    private String communicationTopic;
    private EmailCommunicationStatus communicationStatus;
    private EntityStatus status;
    private String communicationChannel;
    private String emailSubject;
    private String name;

    public EmailCommunicationListingResponse(EmailCommunicationListingMiddleResponse middleResponse) {
        this.massOrIndemailCommunicationId = middleResponse.getMassOrIndemailCommunicationId();
        this.linkedEmailCommunicationId = middleResponse.getLinkedEmailCommunicationId();
        this.customerName = middleResponse.getCustomerName();
        this.uicOrPersonalNumber = middleResponse.getUicOrPersonalNumber();
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
        this.communicationChannel = "EMAIL".equals(middleResponse.getCommunicationChannel()) ? "Individual" : "Mass";
        this.emailSubject = middleResponse.getEmailSubject();
        this.name=middleResponse.getName();
    }
}
