package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResultChangeRequest {
    @NotNull(message = "Process result id is mandatory!;")
    private Long processResultId;
    @NotNull(message = "Balancing group coordinator grounds id is mandatory!;")
    private Long balancingGroupCoordinatorGroundsId;
    @NotNull(message = "Grounds for objection withdrawal id is mandatory!;")
    private Long groundsForObjectionWithdrawalId;

    private boolean check;
}
