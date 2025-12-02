package bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship;

import lombok.Getter;

@Getter
public enum CustomerRelatedRelationshipSearchField {
    ALL("ALL"),
    COMMUNICATION_ID("COMMUNICATION_ID"),
    LINKED_MASS_COMMUNICATION_ID("LINKED_MASS_COMMUNICATION_ID"),
    COMMUNICATION_DATA("COMMUNICATION_DATA"),
    DMS_NUMBER("DMS_NUMBER"),
    EMAIL_ADDRESS("EMAIL_ADDRESS"),
    EMAIL_SUBJECT("SUBJECT"),
    PHONE_NUMBER("PHONE_NUMBER");

    private final String value;

    CustomerRelatedRelationshipSearchField(String value) {
        this.value = value;
    }

}
