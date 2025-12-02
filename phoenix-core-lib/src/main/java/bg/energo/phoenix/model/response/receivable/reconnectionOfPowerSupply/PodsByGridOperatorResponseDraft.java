package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import java.time.LocalDate;

public interface PodsByGridOperatorResponseDraft extends PodsByGridOperatorResponse{
    String getCancellationReason();
    LocalDate getReconnectionDate();
    Long getReconnectionPodId();
    Long getReconnectionId();
    Long getCancellationReasonId();
}
