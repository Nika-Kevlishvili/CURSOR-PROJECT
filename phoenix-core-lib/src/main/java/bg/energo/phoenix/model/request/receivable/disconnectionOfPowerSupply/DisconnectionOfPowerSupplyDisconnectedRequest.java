package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DisconnectionOfPowerSupplyDisconnectedRequest(
        @NotNull(message = "customerId-Id can not be null;")
        Long customerId,
        @NotNull(message = "podId-Id can not be null;")
        Long podId,
        @NotNull(message = "gridOperatorTaxesId-Id must not be null")
        Long gridOperatorTaxesId,
        @NotNull(message = "dateOfDisconnection-Date must not be null;")
        LocalDate dateOfDisconnection,
        boolean expressReconnection
) {
}
