package bg.energo.phoenix.model.response.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.KindOfCommunicationSms;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SmsCommunicationListingResponse {

    private Long massOrIndSmsCommunicationId;
    private String customerName;
    private String uicOrPersonalNumber;
    private String activity;
    private String creatorEmployee;
    private String senderEmployee;
    private String contactPurpose;
    private CommunicationType communicationType;
    private String communicationData;
    private LocalDateTime sentReceiveDate;
    private LocalDateTime createDate;
    private String communicationTopic;
    private SmsCommStatus communicationStatus;
    private EntityStatus status;
    private KindOfCommunicationSms communicationChannel;
    private Long linkedMassSmsId;
    private String name;

    public SmsCommunicationListingResponse(SmsCommunicationListingMiddleResponse middleResponse, Sort.Direction activitySorDirection, Sort.Direction contactSortDirection) {
        this.massOrIndSmsCommunicationId=middleResponse.getMassOrIndSmsCommunicationId();
        this.customerName = middleResponse.getCustomerName();
        this.uicOrPersonalNumber = middleResponse.getUicOrPersonalNumber();
        this.activity=activitySorDirection== Sort.Direction.ASC? middleResponse.getActivityAsc() : middleResponse.getActivityDesc();

        this.creatorEmployee = middleResponse.getCreatorEmployee();
        this.senderEmployee = middleResponse.getSenderEmployee();
        this.contactPurpose= contactSortDirection== Sort.Direction.ASC ? middleResponse.getContactPurposeAsc() : middleResponse.getContactPurposeDesc();
        this.communicationType = middleResponse.getCommunicationtype();
        this.communicationData = middleResponse.getCommunicationData();
        this.sentReceiveDate = middleResponse.getSentReceiveDate();
        this.createDate = middleResponse.getCreateDate();
        this.communicationTopic = middleResponse.getCommunicationTopic();
        this.communicationStatus = middleResponse.getCommunicationStatus();
        this.status = middleResponse.getStatus();
        this.communicationChannel=middleResponse.getCommunicationChannel().getKindOfCommunicationSms();
        this.linkedMassSmsId = middleResponse.getLinkedSmsCommunicationId();
        this.name=middleResponse.getName();
    }
}
