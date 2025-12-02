package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply;

import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@DisconnectionOfPowerSupplyRequestValidator
public record DisconnectionOfPowerSupplyRequest(
        @NotNull(message = "requestForDisconnectionId-Id can not be null;")
        Long requestForDisconnectionId,
        @NotNull(message = "saveType-Save type can not be null;")
        PowerSupplyDisconnectionStatus saveType,
        @Valid
        List<DisconnectionOfPowerSupplyDisconnectedRequest> disconnectedRequest
) {
}
