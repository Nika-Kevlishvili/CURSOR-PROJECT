package bg.energo.phoenix.model.request.product.termination.terminationGroup.termination;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class BaseTerminationGroupTerminationRequest {

    @NotNull(message = "terminationsList.terminationId-Termination ID must not be null;")
    private Long terminationId;

}
