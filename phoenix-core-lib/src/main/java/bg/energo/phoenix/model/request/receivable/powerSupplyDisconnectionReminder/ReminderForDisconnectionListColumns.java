package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

public enum ReminderForDisconnectionListColumns {

    NUMBER("id"),
    CREATION_DATE("create_date"),
    SENDING_DATE("customer_send_date"),
    STATUS("reminder_status"),
    NUMBER_OF_CUSTOMERS("number_of_customers");

    private final String value;

    ReminderForDisconnectionListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
