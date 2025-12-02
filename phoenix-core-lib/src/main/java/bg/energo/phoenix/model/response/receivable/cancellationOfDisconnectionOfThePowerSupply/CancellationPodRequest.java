package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellationPodRequest extends CancellationPodQueryBaseResponse {
    private Long cancellationReasonId;

    public CancellationPodRequest(Long customerId,Long podId, Long requestForDisconnectionOfPowerSupplyId,Long cancellationReasonId) {
        super(customerId,podId,requestForDisconnectionOfPowerSupplyId);
        this.cancellationReasonId = cancellationReasonId;
    }

    public CancellationPodRequest() {
        super();
        cancellationReasonId=null;
    }
}
