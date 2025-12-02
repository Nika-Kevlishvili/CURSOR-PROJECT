package bg.energo.phoenix.model.request.nomenclature.pod;

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
public class PodAdditionalParametersRequest {
    @NotNull(message = "name-should not be null")
    @NotBlank(message = "name-should not be Blank")
    @Length(min = 1, max = 1024, message = "name-length should be between 1 and 1024")
    public String name;

    @NotNull(message = "status-should not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-should not be null")
    private Boolean defaultSelection;
}
