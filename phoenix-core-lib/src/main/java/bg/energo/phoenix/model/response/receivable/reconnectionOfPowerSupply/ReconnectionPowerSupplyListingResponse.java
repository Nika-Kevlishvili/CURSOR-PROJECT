package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReconnectionPowerSupplyListingResponse {

    private Long id;
    private String reconnectionNumber;
    private LocalDate createDate;
    private ReconnectionStatus reconnectionStatus;
    private String gridOperator;
    private Long numberOfPods;
    private EntityStatus status;
    private String powerSupplyDisconnectionRequestNumber;

    public ReconnectionPowerSupplyListingResponse(ReconnectionPowerSupplyListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.reconnectionNumber = middleResponse.getReconnectionNumber();
        this.createDate = middleResponse.getCreateDate();
        this.reconnectionStatus = middleResponse.getReconnectionStatus();
        this.gridOperator = middleResponse.getGridOperator();
        this.numberOfPods = middleResponse.getNumberOfPods();
        this.status = middleResponse.getStatus();
        this.powerSupplyDisconnectionRequestNumber = middleResponse.getPowerSupplyDisconnectionRequestNumber();
    }

}
