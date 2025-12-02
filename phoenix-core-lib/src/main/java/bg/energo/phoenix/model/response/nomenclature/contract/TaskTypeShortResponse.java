package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskTypeShortResponse {
    private Long id;
    private String name;

    public TaskTypeShortResponse(TaskType taskType) {
        this.id = taskType.getId();
        this.name = taskType.getName();
    }
}
