package bg.energo.phoenix.model.request.product.penalty.penaltyGroup;

import bg.energo.phoenix.model.customAnotations.product.penalty.ValidPenaltyGroupUpdateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@ValidPenaltyGroupUpdateRequest
public class PenaltyGroupUpdateRequest {

    @NotBlank(message = "name-Name should not be blank")
    private String name;

    private List<Long> penalties;

    private LocalDate startDate;

    @NotNull(message = "updateExistingVersion-[updateExistingVersion] field must not be null;")
    private Boolean updateExistingVersion;

    @NotNull(message = "versionId-[versionId] field must not be null;")
    private Integer versionId;

}
