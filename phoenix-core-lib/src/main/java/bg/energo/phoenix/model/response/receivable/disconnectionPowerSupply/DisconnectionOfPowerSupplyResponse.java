package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;
import bg.energo.phoenix.model.response.task.TaskShortResponse;

import java.util.List;

public record DisconnectionOfPowerSupplyResponse(
        Long disconnectionNumber,
        PowerSupplyDisconnectionStatus disconnectionStatus,
        Long powerSupplyDisconnectionRequestId,
        List<TaskShortResponse> tasks,
        EntityStatus status
) {
}
