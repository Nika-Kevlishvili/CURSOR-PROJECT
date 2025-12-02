package bg.energo.phoenix.model.response.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReminderListingResponse {
    private Long id;
    private String number;
    private LocalDate creationDate;
    private EntityStatus entityStatus;
    private TriggerForLiabilities triggerForLiabilities;
    private ReminderConditionType reminderConditionType;
    private List<CommunicationChannel> communicationChannels;

    public ReminderListingResponse(ReminderListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.number = middleResponse.getNumber();
        this.entityStatus = middleResponse.getStatus();
        this.creationDate = middleResponse.getCreationDate();
        this.triggerForLiabilities = middleResponse.getTriggerForLiabilities();
        this.reminderConditionType = middleResponse.getConditionType();
        this.communicationChannels = EPBListUtils.convertDBEnumStringArrayIntoListEnum(CommunicationChannel.class, middleResponse.getCommunicationChannel());
    }

}
