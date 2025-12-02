package bg.energo.phoenix.model.request.product.termination.terminationGroup;

import bg.energo.phoenix.model.request.product.termination.terminationGroup.termination.CreateTerminationGroupTerminationRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateTerminationGroupRequest extends BaseTerminationGroupRequest {

    private List<@Valid CreateTerminationGroupTerminationRequest> terminationsList;

}
