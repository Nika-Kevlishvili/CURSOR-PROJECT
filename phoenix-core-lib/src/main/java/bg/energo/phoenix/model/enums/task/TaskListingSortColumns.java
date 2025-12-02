package bg.energo.phoenix.model.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskListingSortColumns {
    ID("id"),
    TASK_TYPE("type"),
    CURRENT_PERFORMER("currentPerformer"),
    PERFORMER("performer"),
    START_DATE("startDate"),
    END_DATE("endDate"),
    COMPLETION_DATE("completionDate"),
    CONNECTION_TYPE("connectionType"),
    STATUS("status"),
    CURRENT_STATUS("currentStatus"),
    CREATE_DATE("createDate");

    private final String name;
}
