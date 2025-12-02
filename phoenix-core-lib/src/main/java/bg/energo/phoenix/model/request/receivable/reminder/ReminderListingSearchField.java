package bg.energo.phoenix.model.request.receivable.reminder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReminderListingSearchField {

    ALL("ALL"),

    REMINDER_NUMBER("REMINDER_NUMBER");

    private final String value;
}
