package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeactivationPurposeRequest {
    @NotBlank(message = "name-should not be Blank;")
    @Length(min = 1, max = 512, message = "name-length should be between {min} and {max};")
    private String name;

    @NotNull(message = "status-should not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-should not be null;")
    private Boolean defaultSelection;
}
