package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import java.time.LocalDate;

public record DisconnectionPowerSupplyParsedFile(
        Long podId,
        String podIdentifier,
        Long disconnectionTypeId,
        String disconnectionType,
        LocalDate disconnectionDate
) {
}
