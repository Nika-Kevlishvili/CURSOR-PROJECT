package bg.energo.phoenix.model.response.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;

import java.time.LocalDate;

public interface ReminderListingMiddleResponse {
    Long getId();

    String getNumber();

    TriggerForLiabilities getTriggerForLiabilities();

    ReminderConditionType getConditionType();

    String getCommunicationChannel();

    LocalDate getCreationDate();

    EntityStatus getStatus();
}
