package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerShortResponse;
import lombok.Data;

@Data
public class TaskTypeStageResponse {
    private Long id;
    private Integer stage;
    private AccountManagerShortResponse performer;
    private Long term;
    private TermType termType;
    private PortalTagResponse tagPerformer;
    private PerformerType performerType;;

}
