package bg.energo.phoenix.model.request.nomenclature.customer;

import bg.energo.phoenix.model.customAnotations.nomenclature.NameDefaultValidator;
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
public class AccountManagerTypeRequest {
    @NotBlank(message = "name-Name should not be null")
    @Size(min = 1, max = 256, message = "name-Name does not match the allowed length")
    @NameDefaultValidator
    private String name;

    @NotNull(message = "status-Status should not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection should not be null")
    private Boolean defaultSelection;
}
