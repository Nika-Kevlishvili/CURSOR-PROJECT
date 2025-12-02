package bg.energo.phoenix.model.request.nomenclature.receivable;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReasonForDisconnectionRequest {

    @NotBlank(message = "name-[name] name is mandatory!")
    @Size(min = 1, max = 1024, message = "name-[Name] does not match the allowed length, range: [1-024];")
    private String name;

    @NotNull(message = "status-[status] status is mandatory")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-[defaultSelection] is mandatory")
    private Boolean defaultSelection;

}
