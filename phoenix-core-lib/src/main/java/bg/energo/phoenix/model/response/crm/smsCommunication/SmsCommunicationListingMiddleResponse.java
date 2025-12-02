package bg.energo.phoenix.model.response.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;

import java.time.LocalDateTime;

public interface SmsCommunicationListingMiddleResponse {
    Long getMassOrIndSmsCommunicationId();
    String getCustomerName();
    String getUicOrPersonalNumber();
    String getActivityDesc();
    String getActivityAsc();
    String getCreatorEmployee();
    String getSenderEmployee();
    String getContactPurposeDesc();
    String getContactPurposeAsc();
    CommunicationType getCommunicationtype();
    String getCommunicationData();
    LocalDateTime getSentReceiveDate();
    LocalDateTime getCreateDate();
    String getCommunicationTopic();
    SmsCommStatus getCommunicationStatus();
    EntityStatus getStatus();
    SmsCommunicationChannel getCommunicationChannel();
    Long getLinkedSmsCommunicationId();
    String getName();
}
