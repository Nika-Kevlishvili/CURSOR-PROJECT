package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.enums.task.PerformerType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TaskPerformerRequest {
    private Integer stage;
    private Long performer;
    private PerformerType performerType;
    private Long term;
    private LocalDate startDate;
}
