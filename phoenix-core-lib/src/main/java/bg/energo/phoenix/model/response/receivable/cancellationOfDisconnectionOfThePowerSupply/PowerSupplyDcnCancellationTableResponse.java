package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import lombok.Data;

@Data
public class PowerSupplyDcnCancellationTableResponse {

    private String customer;

    private String podIdentifier;

    private Long customerId;

    private Long podId;

    private String cancellationReasonName;

    private Long cancellationReasonId;

    private Long requestForDisconnectionId;

    private boolean isChecked;
    private boolean unableToCheck;

    public PowerSupplyDcnCancellationTableResponse(PowerSupplyDcnCancellationTableMiddleResponse middleResponse) {
        this.customer = middleResponse.getCustomer();
        this.podIdentifier = middleResponse.getPodIdentifier();
        this.customerId = middleResponse.getCustomerId();
        this.podId = middleResponse.getPodId();
        this.cancellationReasonName = middleResponse.getCancellationReasonName();
        this.cancellationReasonId = middleResponse.getCancellationReasonId();
        this.isChecked = middleResponse.getIsChecked();
        this.unableToCheck = middleResponse.getUnableToUncheck();
        this.requestForDisconnectionId = middleResponse.getRequestForDisconnectionId();
    }
}
