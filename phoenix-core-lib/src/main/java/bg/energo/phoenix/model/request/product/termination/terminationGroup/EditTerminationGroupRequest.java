package bg.energo.phoenix.model.request.product.termination.terminationGroup;

import bg.energo.phoenix.model.customAnotations.product.terminationGroup.ValidEditTerminationGroupRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.termination.EditTerminationGroupTerminationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidEditTerminationGroupRequest
public class EditTerminationGroupRequest extends BaseTerminationGroupRequest{

    @NotNull(message = "versionId-[versionId] field must not be null;")
    private Long versionId;

    @NotNull(message = "updateExistingVersion-[updateExistingVersion] field must not be null;")
    private Boolean updateExistingVersion;

    private LocalDate startDate;

    private List<@Valid EditTerminationGroupTerminationRequest> terminationsList;

}
