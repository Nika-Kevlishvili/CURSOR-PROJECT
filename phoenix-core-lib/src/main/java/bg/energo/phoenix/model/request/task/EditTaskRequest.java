package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.customAnotations.task.TaskPerformersValidator;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EditTaskRequest {
    @NotEmpty(message = "taskPerformerRequests-Task Performers must not be empty;")
    @TaskPerformersValidator(isUpdate = true)
    private List<TaskPerformerRequest> taskPerformerRequests;
    private String newComment;
}
