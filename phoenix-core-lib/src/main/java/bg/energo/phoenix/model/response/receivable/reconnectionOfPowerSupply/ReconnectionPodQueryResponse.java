package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconnectionPodQueryResponse extends ReconnectionPodQueryBaseResponse{
    private Boolean checked;
    private Long reconnectionPodId;
    private Boolean unableToUncheck;

    public ReconnectionPodQueryResponse(Long customerId,Long podId, Long requestForDisconnectionOfPowerSupplyId,Boolean checked,Boolean unableToUncheck) {
        super(customerId,podId,requestForDisconnectionOfPowerSupplyId);
        this.checked = checked;
        this.unableToUncheck=unableToUncheck;
    }

    public ReconnectionPodQueryResponse() {
        super();
        this.checked = false;
        this.unableToUncheck=false;
    }

    public ReconnectionPodQueryResponse(PodsByGridOperatorResponse response) {
        this.setPodId(response.getPodId());
        this.setCustomerId(response.getCustomerId());
        this.setChecked(response.getIsChecked());
        this.setRequestForDisconnectionOfPowerSupplyId(response.getRequestForDisconnectionId());
        this.checked=response.getIsChecked();
        this.unableToUncheck=response.getUnableToUncheck();
    }

    public ReconnectionPodQueryResponse(PodsByGridOperatorResponseDraft response) {
        this.setPodId(response.getPodId());
        this.setCustomerId(response.getCustomerId());
        this.setChecked(response.getIsChecked());
        this.setRequestForDisconnectionOfPowerSupplyId(response.getRequestForDisconnectionId());
        this.checked=response.getIsChecked();
        this.reconnectionPodId= response.getReconnectionPodId();
        this.unableToUncheck = response.getUnableToUncheck();
    }
}
