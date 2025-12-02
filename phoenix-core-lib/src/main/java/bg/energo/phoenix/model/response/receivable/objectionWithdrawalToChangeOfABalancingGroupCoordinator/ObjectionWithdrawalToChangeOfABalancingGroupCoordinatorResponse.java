package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder(setterPrefix = "with")
public class ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse {

    private Long id;
    private String number;
    private LocalDate creationDate;
    private ObjectionWithdrawalToChangeOfCbgStatus objectionWithdrawalToChangeOfCbgStatus;
    private ObjectionToChangeOfCbgShortResponse objectionToChangeOfCbg;
    private List<TaskShortResponse> tasks;
    private EntityStatus status;
    private List<ContractTemplateShortResponse> templateResponses;
    private ContractTemplateShortResponse emailTemplateResponse;
    private List<ObjectionWithdrawalToCbgFileResponse> files;
}
