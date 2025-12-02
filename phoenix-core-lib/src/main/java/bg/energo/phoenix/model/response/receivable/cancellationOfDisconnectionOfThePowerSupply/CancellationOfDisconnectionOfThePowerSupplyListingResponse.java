package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CancellationOfDisconnectionOfThePowerSupplyListingResponse {
    private Long id;
    private CancellationOfDisconnectionOfThePowerSupplyStatus cancellationStatus;
    private String requestForDisconnectionOfThePowerSupplyNumber;
    private String cancellationNumber;
    private LocalDate cancellationDate;
    private EntityStatus entityStatus;
    private Integer numberOfPods;

    public CancellationOfDisconnectionOfThePowerSupplyListingResponse(CancellationOfDisconnectionOfThePowerSupplyListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.entityStatus = middleResponse.getEntityStatus();
        this.cancellationDate = middleResponse.getCancellationDate();
        this.cancellationStatus = middleResponse.getCancellationStatus();
        this.cancellationNumber = middleResponse.getCancellationNumber();
        this.requestForDisconnectionOfThePowerSupplyNumber = middleResponse.getRequestForDisconnectionNumber();
        this.numberOfPods = middleResponse.getNumberOfPods();
    }
}
