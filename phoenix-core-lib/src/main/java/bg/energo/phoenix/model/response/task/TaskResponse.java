package bg.energo.phoenix.model.response.task;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeShortResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private Long number;
    private String currentPerformer;
    private String currentPerformerUsername;
    private Integer currentTaskStageNumber;
    private TaskTypeShortResponse taskType;
    private TaskStatus taskStatus;
    private EntityStatus status;
    private TaskConnectionType connectionType;
    private String description;
    private List<TaskCommentHistory> commentHistory;
    private List<TaskConnectedEntityResponse> connectedEntities;
    private List<TaskStageResponse> taskStages;
    private List<SystemActivityShortResponse> activities;
}
