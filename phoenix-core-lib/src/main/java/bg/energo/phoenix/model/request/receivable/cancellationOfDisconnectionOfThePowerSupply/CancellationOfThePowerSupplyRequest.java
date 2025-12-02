package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationPodRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@CancellationOfPowerSupplyRequestValidator
public class CancellationOfThePowerSupplyRequest {


    @NotNull(message = "requestForDisconnectionOfThePowerSupplyId-[requestForDisconnectionOfThePowerSupplyId] must not be null;")
    private Long requestForDisconnectionOfThePowerSupplyId;

    @NotNull(message = "saveAs-[saveAs] save as is mandatory!;")
    private CancellationOfDisconnectionOfThePowerSupplyStatus saveAs;

    private Set<Long> fileIds = Collections.emptySet();

    private Set<CancellationPodRequest> table = Collections.emptySet();

    private Set<Long> templateIds;

    //TODO: Template subObject

}
