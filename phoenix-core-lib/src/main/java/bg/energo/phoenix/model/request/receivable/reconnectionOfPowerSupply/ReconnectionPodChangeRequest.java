package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
public class ReconnectionPodChangeRequest {
    @NotNull(message = "reconnection pod id is mandatory!;")
    private Long reconnectionPodId;

    @NotNull(message = "cancellation reason is mandatory!;")
    private Long cancellationReasonId;

    private LocalDate reconnectionDate;

    public int hashCode() {
        return Objects.hash(reconnectionPodId);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if(!(o instanceof ReconnectionPodChangeRequest other)) {
            return false;
        }
        return Objects.equals(reconnectionPodId, other.reconnectionPodId);
    }
}
