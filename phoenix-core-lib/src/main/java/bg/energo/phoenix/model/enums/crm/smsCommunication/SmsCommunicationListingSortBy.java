package bg.energo.phoenix.model.enums.crm.smsCommunication;

public enum SmsCommunicationListingSortBy {
    ID("massOrIndSmsCommunicationId"),
    CUSTOMER_NAME("customerName"),
    UIC_PERSONAL_NUMBER("uicOrPersonalNumber"),
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
    ENTITY_STATUS("status"),
    COMMUNICATION_CHANNEL("communicationChannel"),
    LINKED_MASS_COMMUNICATION("linkedSmsCommunicationId")
    ;

    private final String value;

    private SmsCommunicationListingSortBy(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

}
