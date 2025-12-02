package bg.energo.phoenix.model.request.receivable.reminder;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class ReminderListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private ReminderListingSearchField searchField;

    private List<TriggerForLiabilities> triggerForLiabilities;

    private List<ReminderConditionType> reminderConditionTypes;

    private List<CommunicationChannel> communicationChannels;

    private Sort.Direction direction;

    private ReminderListingColumn column;
}
