package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;

import java.time.LocalDate;

public interface DisconnectionPowerSupplyListingMiddleResponse {

    Long getId();

    String getDisconnectionNumber();

    LocalDate getCreateDate();

    PowerSupplyDisconnectionStatus getDisconnectionStatus();

    Long getNumberOfPods();

    EntityStatus getStatus();

    String getRequestForDisconnectionNumber();

}
