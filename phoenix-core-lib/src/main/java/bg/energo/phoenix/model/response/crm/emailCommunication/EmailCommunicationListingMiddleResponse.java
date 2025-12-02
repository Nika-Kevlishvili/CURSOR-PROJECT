package bg.energo.phoenix.model.response.crm.emailCommunication;


import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;

import java.time.LocalDateTime;

public interface EmailCommunicationListingMiddleResponse {
    Long getMassOrIndemailCommunicationId();
    Long getLinkedEmailCommunicationId();
    String getCustomerName();
    String getUicOrPersonalNumber();
    String getActivity();
    String getCreatorEmployee();
    String getSenderEmployee();
    String getContactPurpose();
    EmailCommunicationType getCommunicationType();
    String getCommunicationData();
    LocalDateTime getSentReceiveDate();
    LocalDateTime getCreateDate();
    String getCommunicationTopic();
    EmailCommunicationStatus getCommunicationStatus();
    EntityStatus getStatus();
    String getCommunicationChannel();
    String getEmailSubject();
    String getName();
}
