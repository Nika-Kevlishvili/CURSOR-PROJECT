package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskTypeStageRequest {
    private Long id;
    private Integer stage;
    private Long performerId;
    private PerformerType performerType;
    private Long term;
    private TermType termType;
}
