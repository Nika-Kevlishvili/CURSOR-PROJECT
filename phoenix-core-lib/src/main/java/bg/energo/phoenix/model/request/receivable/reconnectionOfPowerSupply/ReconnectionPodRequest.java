package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.ReconnectionPodQueryBaseResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconnectionPodRequest extends ReconnectionPodQueryBaseResponse {
   private Long cancellationReasonId;

    public ReconnectionPodRequest(Long customerId,Long podId, Long requestForDisconnectionOfPowerSupplyId,Long cancellationReasonId) {
        super(customerId,podId,requestForDisconnectionOfPowerSupplyId);
        this.cancellationReasonId = cancellationReasonId;
    }

    public ReconnectionPodRequest() {
        super();
        cancellationReasonId=null;
    }
}
