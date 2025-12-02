package bg.energo.phoenix.process.model.request;

import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.process.model.enums.ProcessNotificationType;
import lombok.Data;

@Data
public class ProcessNotificationObject {
    private ProcessNotificationType type;
    private PerformerType performerType;
    private Long performerId;
}
