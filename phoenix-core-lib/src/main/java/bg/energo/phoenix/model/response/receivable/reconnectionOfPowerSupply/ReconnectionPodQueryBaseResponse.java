package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ReconnectionPodQueryBaseResponse {
    private Long customerId;
    private Long podId;
    private Long requestForDisconnectionOfPowerSupplyId;

    @Override
    public boolean equals(Object object) {
        if(object==this) {
            return true;
        }

        if(!(object instanceof ReconnectionPodQueryBaseResponse other)) {
            return false;
        }

        return Objects.equals(this.customerId, other.customerId) &&
                Objects.equals(this.podId, other.podId) &&
                Objects.equals(this.requestForDisconnectionOfPowerSupplyId, other.requestForDisconnectionOfPowerSupplyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId,podId,requestForDisconnectionOfPowerSupplyId);
    }

    public String toString() {
        return "Customer id : " + customerId + ",POD id : " + podId + ", Request for disconnection of power supply id : " + requestForDisconnectionOfPowerSupplyId;
    }
}
