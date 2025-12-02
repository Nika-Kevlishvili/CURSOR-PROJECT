package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import java.time.LocalDate;

public record DisconnectionPowerSupplyTableList(
        String customer,
        String podIdentifier,
        String disconnectionType,
        LocalDate disconnectionDate,
        Boolean isChecked,
        Boolean expressReconnection,
        Long customerId,
        Long podId,
        Long pspdId,
        Long disconnectionId,
        Long gridOperatorTaxId,
        Long powerSupplyDisconnectionId
) {
    public DisconnectionPowerSupplyTableList(DisconnectionPowerSupplyPodsMiddleResponse middleResponse) {
        this(
                middleResponse.getCustomer(),
                middleResponse.getPodIdentifier(),
                middleResponse.getDisconnectionType(),
                middleResponse.getDisconnectionDate(),
                middleResponse.getIsChecked(),
                middleResponse.getExpressReconnection(),
                middleResponse.getCustomerId(),
                middleResponse.getPodId(),
                middleResponse.getPsdpId(),
                middleResponse.getDisconnectionId(),
                middleResponse.getGridOperatorTaxId(),
                middleResponse.getPowerSupplyDisconnectionId());
    }
}
