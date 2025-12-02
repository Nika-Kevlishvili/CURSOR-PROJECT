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
public class CiConnectionTypeRequest {

    @NotBlank(message = "name-Name shouldn't be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length {min}:{max} characters;")
    @NameDefaultValidator
    private String name;

    @NotNull(message = "status-Status shouldn't be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection shouldn't be null;")
    private Boolean defaultSelection;

}
