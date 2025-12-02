package bg.energo.phoenix.model.response.receivable.rescheduling;

import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TasksResponse {
    private Long id;
    private Long number;
    private TaskTypeShortResponse taskType;
    private LocalDateTime createDate;
}
