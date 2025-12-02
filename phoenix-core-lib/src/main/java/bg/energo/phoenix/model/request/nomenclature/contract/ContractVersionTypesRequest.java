package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractVersionTypesRequest {
    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 512, message = "name-Name has invalid length: [{min}:{max}]")
    private String name;

    @NotNull(message = "status-Status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null")
    private Boolean defaultSelection;
}
