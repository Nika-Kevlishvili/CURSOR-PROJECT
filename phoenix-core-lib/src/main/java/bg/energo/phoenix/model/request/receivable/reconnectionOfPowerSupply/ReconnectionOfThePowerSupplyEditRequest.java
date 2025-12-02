package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@ReconnectionOfThePowerSupplyEditRequestValidator
public class ReconnectionOfThePowerSupplyEditRequest extends ReconnectionOfThePowerSupplyBaseRequest {

    private Long fileId;

    private @Valid Set<ReconnectionPodChangeRequest> existingPodChangeRequest = Collections.emptySet();

    public ReconnectionOfThePowerSupplyEditRequest() {
        super();
    }


    @JsonSetter
    public void setExistingPodChangeRequest(Set<ReconnectionPodChangeRequest> existingPodChangeRequest) {
        this.existingPodChangeRequest = existingPodChangeRequest != null ? existingPodChangeRequest : Collections.emptySet();
    }
}
