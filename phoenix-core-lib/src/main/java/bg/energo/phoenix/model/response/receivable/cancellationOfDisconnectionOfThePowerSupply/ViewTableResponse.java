package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ViewTableResponse {
    private String customer;

    private String podIdentifier;

    private Long customerId;

    private Long podId;

    private Long requestForDisconnectionId;

    private boolean checked;

    private boolean unableToUncheck;

    private String cancellationReason;

    private Long cancellationPodId;

    private Long cancellationReasonId;

    public ViewTableResponse(PodsByRequestOfDcnResponseDraft response) {
        this.customer = response.getCustomer();
        this.podIdentifier = response.getPodIdentifier();
        this.customerId = response.getCustomerId();
        this.podId = response.getPodId();
        this.requestForDisconnectionId = response.getRequestForDisconnectionId();
        this.checked = response.getIsChecked();
        this.unableToUncheck = response.getUnableToUncheck();
        this.cancellationReason = response.getCancellationReasonName();
        this.cancellationPodId = response.getCancellationPodId();
        this.cancellationReasonId = response.getCancellationReasonId();
    }
}
