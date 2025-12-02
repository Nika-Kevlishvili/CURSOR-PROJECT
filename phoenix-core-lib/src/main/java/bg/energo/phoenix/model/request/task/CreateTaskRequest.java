package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.customAnotations.task.TaskConnectedEntitiesValidator;
import bg.energo.phoenix.model.customAnotations.task.TaskPerformerRequestValidator;
import bg.energo.phoenix.model.customAnotations.task.TaskPerformersValidator;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@TaskConnectedEntitiesValidator
public class CreateTaskRequest {
    @NotNull(message = "taskTypeId-Task Type must not be null;")
    private Long taskTypeId;

    @NotNull(message = "connectionType-Connection Type must not be null;")
    private TaskConnectionType connectionType;

    @TaskPerformersValidator
    @NotEmpty(message = "taskPerformerRequests-Task Performers must not be empty;")
    @TaskPerformerRequestValidator
    private  List<TaskPerformerRequest> taskPerformerRequests;

    @NotBlank(message = "description-Description must not be blank;")
    private String description;

    private String newComment;

    private List<@Valid TaskConnectedEntity> connectedEntities;
}
