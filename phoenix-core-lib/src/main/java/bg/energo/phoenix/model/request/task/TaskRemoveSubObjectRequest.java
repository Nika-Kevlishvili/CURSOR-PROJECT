package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.enums.task.TaskSubObjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskRemoveSubObjectRequest {
    @NotNull(message = "id-Task Sub Object ID must not be null;")
    private Long subObjectId;

    @NotNull(message = "taskSubObjectType-Task Sub Object Type must not be null;")
    private TaskSubObjectType taskSubObjectType;
}
