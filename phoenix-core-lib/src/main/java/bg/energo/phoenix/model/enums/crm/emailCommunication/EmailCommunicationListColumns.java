package bg.energo.phoenix.model.enums.crm.emailCommunication;

public enum EmailCommunicationListColumns {
    ID("massOrIndemailCommunicationId"),
    LINKED_EMAIL_COMMUNICATION("linkedEmailCommunicationId"),
    CUSTOMER_NAME("customerName"),
    UIC_PERSONAL_NUMBER("uicOrPersonalNumber"),
    ACTIVITY("activity"),
    CREATOR_EMPLOYEE("creatorEmployee"),
    SENDER_EMPLOYEE("senderEmployee"),
    CONTACT_PURPOSE("contactPurpose"),
    COMMUNICATION_TYPE("communicationType"),
    COMMUNICATION_DATA("communicationData"),
    SENT_RECEIVE_DATE_TIME("sentReceiveDate"),
    CREATED_DATE_TIME("createDate"),
    COMMUNICATION_TOPIC("communicationTopic"),
    COMMUNICATION_STATUS("communicationStatus"),
    STATUS("status"),
    SUBJECT("emailSubject"),
    COMMUNICATION_CHANNEL("communicationChannel");

    private final String value;

    EmailCommunicationListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
