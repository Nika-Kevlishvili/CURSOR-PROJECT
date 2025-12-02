package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellationPodQueryResponse extends CancellationPodQueryBaseResponse {
    private Boolean checked;
    private Long cancellationPodId;

    public CancellationPodQueryResponse(Long customerId,Long podId, Long requestForDisconnectionOfPowerSupplyId,Boolean checked) {
        super(customerId,podId,requestForDisconnectionOfPowerSupplyId);
        this.checked = checked;
    }

    public CancellationPodQueryResponse() {
        super();
        this.checked = false;
    }

    public CancellationPodQueryResponse(PowerSupplyDcnCancellationTableMiddleResponse response) {
        this.setPodId(response.getPodId());
        this.setCustomerId(response.getCustomerId());
        this.setChecked(response.getIsChecked());
        this.setRequestForDisconnectionOfPowerSupplyId((response.getRequestForDisconnectionId()));
        this.checked=response.getIsChecked();
    }

    public CancellationPodQueryResponse(PodsByRequestOfDcnResponseDraft response) {
        this.setPodId(response.getPodId());
        this.setCustomerId(response.getCustomerId());
        this.setChecked(response.getIsChecked());
        this.setRequestForDisconnectionOfPowerSupplyId((response.getRequestForDisconnectionId()));
        this.checked=response.getIsChecked();
        this.cancellationPodId= response.getCancellationPodId();
    }
}
