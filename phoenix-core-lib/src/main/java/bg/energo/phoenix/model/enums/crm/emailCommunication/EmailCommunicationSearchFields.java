package bg.energo.phoenix.model.enums.crm.emailCommunication;

import lombok.Getter;

public enum EmailCommunicationSearchFields {
    ALL("ALL"),
    COMMUNICATION_ID("COMMUNICATION_ID"),
    LINKED_MASS_COMMUNICATION_ID("LINKED_MASS_COMMUNICATION_ID"),
    COMMUNICATION_DATA("COMMUNICATION_DATA"),
    CUSTOMER_NAME("CUSTOMER_NAME"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    EMAIL_SUBJECT("EMAIL_SUBJECT"),
    EMAIL_ADDRESS("EMAIL_ADDRESS"),
    DMS_NUMBER("DMS_NUMBER");

    @Getter
    private String value;

    EmailCommunicationSearchFields(String value) {
        this.value = value;
    }
}
