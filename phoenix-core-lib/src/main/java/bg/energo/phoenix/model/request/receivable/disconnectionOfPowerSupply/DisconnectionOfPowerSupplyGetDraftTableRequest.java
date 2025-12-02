package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply;

import jakarta.validation.constraints.NotNull;

public record DisconnectionOfPowerSupplyGetDraftTableRequest(
        @NotNull(message = "page-Page must not be null;")
        Integer page,
        @NotNull(message = "size-Size must not be null;")
        Integer size,
        DisconnectionOfPowerSupplySearchFields searchField,
        String prompt,
        @NotNull(message = "requestForDisconnectionId-Id must not be null;")
        Long requestForDisconnectionId,
        Long disconnectionId
) {
}
