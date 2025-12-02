package bg.energo.phoenix.model.response.task;

import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.enums.task.TaskStageStatus;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerShortResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class TaskStageResponse {
    private Long id;
    private AccountManagerShortResponse performer;
    private AccountManagerShortResponse currentPerformer;
    private PortalTagResponse tagPerformer;
    private PerformerType performerType;
    private Long term;
    private TermType termType;
    private Integer stage;
    private TaskStageStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate completeDate;
}
