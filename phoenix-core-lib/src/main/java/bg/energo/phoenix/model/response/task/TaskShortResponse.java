package bg.energo.phoenix.model.response.task;

import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.task.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskShortResponse {
    private Long id;
    private String name;

    public TaskShortResponse(Task task, TaskType taskType) {
        this.id = task.getId();
        this.name = "(%s) - %s/%s".formatted(this.id, task.getCreateDate().toLocalDate(), taskType.getName());
    }
}
