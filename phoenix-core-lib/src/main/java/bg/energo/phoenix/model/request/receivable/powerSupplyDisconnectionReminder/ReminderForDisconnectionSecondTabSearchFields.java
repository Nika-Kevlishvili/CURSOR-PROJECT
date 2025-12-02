package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import lombok.Getter;

public enum ReminderForDisconnectionSecondTabSearchFields {

    ALL("ALL"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    LIABILITY_NUMBER("LIABILITY_NUMBER"),
    OUTGOING_DOCUMENT_NUMBER("OUTGOING_DOCUMENT_NUMBER");

    @Getter
    private String value;

    ReminderForDisconnectionSecondTabSearchFields(String value) {
        this.value = value;
    }
}
