package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateReconnectionTableResponse {

    private String customer;

    private Long customerId;

    private String podIdentifier;

    private Long podId;

    private String requestForDisconnectionNumber;

    private Long requestForDisconnectionId;

    private boolean isChecked;

    private boolean unableToUncheck;

    public CreateReconnectionTableResponse(PodsByGridOperatorResponse podsByGridOperatorResponse) {
        this.customer = podsByGridOperatorResponse.getCustomer();
        this.customerId = podsByGridOperatorResponse.getCustomerId();
        this.podIdentifier = podsByGridOperatorResponse.getPodIdentifier();
        this.podId = podsByGridOperatorResponse.getPodId();
        this.requestForDisconnectionNumber=podsByGridOperatorResponse.getPsdrRequestNumber();
        this.requestForDisconnectionId=podsByGridOperatorResponse.getRequestForDisconnectionId();
        this.isChecked=podsByGridOperatorResponse.getIsChecked();
        this.unableToUncheck=podsByGridOperatorResponse.getUnableToUncheck();
    }
}
