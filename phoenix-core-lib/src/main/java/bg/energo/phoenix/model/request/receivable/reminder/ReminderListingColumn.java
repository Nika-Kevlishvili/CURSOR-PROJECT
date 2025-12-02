package bg.energo.phoenix.model.request.receivable.reminder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReminderListingColumn {
    ID("id"),

    NUMBER("reminder_number"),

    TRIGGER_FOR_LIABILITIES("trigger_for_liabilities"),

    CONDITION_TYPE("customer_condition"),

    COMMUNICATION_CHANNEL("communication_channel"),

    CREATION_DATE("create_date"),

    STATUS("status");

    private final String value;
}
