package bg.energo.phoenix.model.request.product.termination.terminationGroup.termination;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EditTerminationGroupTerminationRequest extends BaseTerminationGroupTerminationRequest {

    private Long id;

}
