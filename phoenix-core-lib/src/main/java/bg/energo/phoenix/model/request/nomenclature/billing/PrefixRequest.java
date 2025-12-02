package bg.energo.phoenix.model.request.nomenclature.billing;

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
public class PrefixRequest {

    @NotBlank(message = "name-Prefix must not blank;")
    @Size(min = 1, max = 3, message = "name-Prefix should be between {min} and {max} characters;")
    @Pattern(regexp = "^[0-9A-ZА-Я]+$", message = "name-Allowed symbols in prefix are: A-Z А-Я 0-9;")
    private String name;

    @NotNull(message = "prefixType-prefixType must not be null;")
    private String prefixType;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;
}
