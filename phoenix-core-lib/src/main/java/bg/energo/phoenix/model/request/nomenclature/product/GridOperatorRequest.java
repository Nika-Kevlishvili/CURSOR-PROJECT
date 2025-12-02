package bg.energo.phoenix.model.request.nomenclature.product;

import bg.energo.phoenix.model.customAnotations.product.product.ValidEmail;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridOperatorRequest {

    @NotBlank(message = "name-Name must not be null or blank")
    @Size(min = 1, max = 512, message = "name-Name must contain min {min} and max {max} symbols")
    private String name;

    @NotBlank(message = "fullName-Full name must not be null or blank")
    @Size(min = 1, max = 512, message = "fullName-Full name must contain min {min} and max {max} symbols")
    private String fullName;

    @ValidEmail(field = "powerSupplyTerminationRequestEmail")
    private String powerSupplyTerminationRequestEmail;

    @ValidEmail(field = "powerSupplyReconnectionRequestEmail")
    private String powerSupplyReconnectionRequestEmail;

    @ValidEmail(field = "objectionToChangeCBGEmail")
    private String objectionToChangeCBGEmail;

    @Pattern(regexp = "^[0-9]*$", message = "codeForXEnergy-xEnergy code has invalid pattern, correct pattern is {regexp};")
    @Size(min = 1, max = 32, message = "codeForXEnergy-xEnergy code length must be in range: [{min}:{max}];")
    private String codeForXEnergy;

    @Pattern(regexp = "^[0-9]*$", message = "gridOperatorCode-Grid Operator Code has invalid pattern, correct pattern is {regexp};")
    @Size(min = 1, max = 32, message = "gridOperatorCode-Grid Operator Code length must be in range: [{min}:{max}];")
    private String gridOperatorCode;

    @NotNull(message = "status-Status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null")
    private Boolean defaultSelection;

    private Boolean ownedByEnergoPro;

}
