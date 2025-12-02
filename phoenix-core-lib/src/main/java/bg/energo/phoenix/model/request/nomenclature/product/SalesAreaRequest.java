package bg.energo.phoenix.model.request.nomenclature.product;

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
public class SalesAreaRequest {

    @NotBlank(message = "name-Name must not be blank")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length")
    private String name;

    @Size(min = 1, max = 512, message = "loginPortalTag-Tag from login portal does not match the allowed length")
    private String loginPortalTag;

    @NotNull(message = "status-status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null")
    private Boolean defaultSelection;

}
