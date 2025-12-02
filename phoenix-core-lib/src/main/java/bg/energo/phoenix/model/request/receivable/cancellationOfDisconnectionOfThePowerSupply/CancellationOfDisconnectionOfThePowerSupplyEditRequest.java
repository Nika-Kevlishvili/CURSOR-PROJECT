package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@CancellationOfDisconnectionOfPowerSupplyRequestValidator
public class CancellationOfDisconnectionOfThePowerSupplyEditRequest extends CancellationOfThePowerSupplyRequest {

    private @Valid Set<CancellationPodChangeRequest> existingPodChangeRequest = Collections.emptySet();

    public CancellationOfDisconnectionOfThePowerSupplyEditRequest() {
        super();
    }

}
