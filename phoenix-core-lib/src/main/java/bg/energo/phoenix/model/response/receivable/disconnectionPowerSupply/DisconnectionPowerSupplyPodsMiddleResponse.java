package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import java.time.LocalDate;

public interface DisconnectionPowerSupplyPodsMiddleResponse {

    String getCustomer();
    String getPodIdentifier();
    String getDisconnectionType();
    LocalDate getDisconnectionDate();
    Boolean getIsChecked();
    Boolean getExpressReconnection();
    Long getCustomerId();
    Long getPodId();
    Long getPsdpId();
    Long getDisconnectionId();
    Long getGridOperatorTaxId();
    Long getPowerSupplyDisconnectionId();
}
