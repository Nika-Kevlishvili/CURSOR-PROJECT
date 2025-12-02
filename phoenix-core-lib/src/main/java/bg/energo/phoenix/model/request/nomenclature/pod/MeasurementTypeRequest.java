package bg.energo.phoenix.model.request.nomenclature.pod;

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
public class MeasurementTypeRequest {

    @NotBlank(message = "name-Measurement Type name must not blank;")
    @Size(min = 1, max = 512, message = "name-Measurement Type name should be between {min} and {max} characters;")
    private String name;

    @NotNull(message = "gridOperator-Grid Operator must not be null;")
    private Long gridOperatorId;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;
}
