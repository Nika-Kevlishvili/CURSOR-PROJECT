package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionTypeRequest {

    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 512, message = "name-Name length should be between {min} and {max} characters;")
    private String name;

    @NotNull(message = "status-Status is mandatory;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection value is mandatory;")
    private Boolean defaultSelection;

}
