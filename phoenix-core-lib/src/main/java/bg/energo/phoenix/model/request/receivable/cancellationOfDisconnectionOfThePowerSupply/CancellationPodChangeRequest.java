package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class CancellationPodChangeRequest {
    @NotNull(message = "reconnection pod id is mandatory!;")
    private Long cancellationPodId;

    @NotNull(message = "cancellation reason is mandatory!;")
    private Long cancellationReasonId;

    public int hashCode() {
        return Objects.hash(cancellationPodId);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if(!(o instanceof CancellationPodChangeRequest other)) {
            return false;
        }
        return Objects.equals(cancellationPodId, other.cancellationPodId);
    }

}
