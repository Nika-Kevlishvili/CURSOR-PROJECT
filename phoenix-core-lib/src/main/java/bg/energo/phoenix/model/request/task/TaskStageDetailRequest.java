package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.enums.contract.TermType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TaskStageDetailRequest {
    private Integer stage;
    private Long term;
    private LocalDate startDate;
    private TermType termType;
}
