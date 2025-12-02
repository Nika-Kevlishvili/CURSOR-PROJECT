package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import lombok.Getter;

public enum ReminderForDisconnectionSearchFields {

    ALL("ALL"),
    NUMBER("REMINDER_NUMBER"),
    CUSTOMER("CUSTOMER_IDENTIFIER");

    @Getter
    private String value;

    ReminderForDisconnectionSearchFields(String value) {
        this.value = value;
    }
}
