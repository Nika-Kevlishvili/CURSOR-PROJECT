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
public class BalancingGroupCoordinatorsRequest {
    
    @NotBlank(message = "name-Name should not be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    private String name;

    @NotBlank(message = "fullName-fullName should not be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    private String fullName;

    @NotNull(message = "status-Status should not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection should not be null;")
    private Boolean defaultSelection;
}
