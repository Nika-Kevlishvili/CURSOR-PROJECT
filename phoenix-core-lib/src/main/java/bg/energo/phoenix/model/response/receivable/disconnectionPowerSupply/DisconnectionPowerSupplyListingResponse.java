package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DisconnectionPowerSupplyListingResponse {

    private Long id;
    private String disconnectionNumber;
    private LocalDate createDate;
    private PowerSupplyDisconnectionStatus disconnectionStatus;
    private Long numberOfPods;
    private EntityStatus status;
    private String requestForDisconnectionNumber;

    public DisconnectionPowerSupplyListingResponse(DisconnectionPowerSupplyListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.disconnectionNumber = middleResponse.getDisconnectionNumber();
        this.createDate = middleResponse.getCreateDate();
        this.disconnectionStatus = middleResponse.getDisconnectionStatus();
        this.numberOfPods = middleResponse.getNumberOfPods();
        this.status = middleResponse.getStatus();
        this.requestForDisconnectionNumber = middleResponse.getRequestForDisconnectionNumber();
    }

}
