package bg.energo.phoenix.model.response.activity;

import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.task.Task;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class TaskActivityShortResponse {
    private Long taskId;
    private String name;

    public TaskActivityShortResponse(Task task, TaskType taskType) {
        this.taskId = task.getId();
        this.name = "%s - %s - %s".formatted(
                task.getNumber(),
                task.getCreateDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                taskType.getName()
        );
    }

}
