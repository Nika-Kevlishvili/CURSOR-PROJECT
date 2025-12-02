package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequest {
    @NotBlank(message = "name-should not be Blank;")
    @Length(min = 1, max = 512, message = "name-length should be between {min} and {max};")
    private String name;

    @NotNull(message = "status-Status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null")
    private Boolean defaultSelection;
}
