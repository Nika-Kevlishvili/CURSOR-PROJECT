package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class TableViewResponse {

    private String customer;

    private String podIdentifier;

    private String requestForDisconnectionNumber;

    private Long customerId;

    private Long podId;

    private Long requestForDisconnectionId;

    private Long gridOperatorId;

    private boolean checked;

    private boolean unableToUncheck;

    private String cancellationReason;

    private LocalDate reconnectionDate;

    private Long reconnectionPodId;

    private Long reconnectionId;

    private Long cancellationReasonId;

    public TableViewResponse(PodsByGridOperatorResponseDraft podsByGridOperatorResponseDraft) {
        this.customer = podsByGridOperatorResponseDraft.getCustomer();
        this.podIdentifier = podsByGridOperatorResponseDraft.getPodIdentifier();
        this.requestForDisconnectionNumber=podsByGridOperatorResponseDraft.getPsdrRequestNumber();
        this.customerId = podsByGridOperatorResponseDraft.getCustomerId();
        this.podId = podsByGridOperatorResponseDraft.getPodId();
        this.requestForDisconnectionId=podsByGridOperatorResponseDraft.getRequestForDisconnectionId();
        this.gridOperatorId = podsByGridOperatorResponseDraft.getGridOperatorId();
        this.checked= podsByGridOperatorResponseDraft.getIsChecked();
        this.unableToUncheck= podsByGridOperatorResponseDraft.getUnableToUncheck();
        this.cancellationReason= podsByGridOperatorResponseDraft.getCancellationReason();
        this.reconnectionDate= podsByGridOperatorResponseDraft.getReconnectionDate();
        this.reconnectionPodId= podsByGridOperatorResponseDraft.getReconnectionPodId();
        this.reconnectionId= podsByGridOperatorResponseDraft.getReconnectionId();
        this.cancellationReasonId= podsByGridOperatorResponseDraft.getCancellationReasonId();
    }


}
