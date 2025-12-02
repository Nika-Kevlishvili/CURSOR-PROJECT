package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;

import java.time.LocalDate;

public interface ReconnectionPowerSupplyListingMiddleResponse {

    Long getId();

    String getReconnectionNumber();

    LocalDate getCreateDate();

    ReconnectionStatus getReconnectionStatus();

    String getGridOperator();

    Long getNumberOfPods();

    EntityStatus getStatus();

    String getPowerSupplyDisconnectionRequestNumber();

}
