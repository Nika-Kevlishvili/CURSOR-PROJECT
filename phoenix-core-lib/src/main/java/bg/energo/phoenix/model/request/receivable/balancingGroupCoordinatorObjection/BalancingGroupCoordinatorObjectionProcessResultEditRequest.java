package bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalancingGroupCoordinatorObjectionProcessResultEditRequest {

    @NotNull(message = "processResultId-[processResultId] must not be null")
    private Long processResultId;

    private Long groundForObjectionWithdrawalToCbgId;

    private Long balancingGroupCoordinatorId;

    private boolean isChecked;
}
