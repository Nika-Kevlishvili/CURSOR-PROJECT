package bg.energo.phoenix.model.request.nomenclature.receivable;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalConditionRequest {

    @NotBlank(message = "name-Name must not be blank")
    @Size(min = 1, max = 1024, message = "name-Name does not match the allowed length")
    private String name;

    @NotNull(message = "customerAssessmentTypeId-customerAssessmentTypeId must not be null")
    private Long customerAssessmentTypeId;

    @NotNull(message = "status-status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null")
    private Boolean defaultSelection;

}
