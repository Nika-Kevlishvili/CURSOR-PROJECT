package bg.energo.phoenix.model.request.nomenclature.billing;

import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeAccountNameRequest {
    @NotBlank(message = "name-Name should not be blank;")
    @Size(min = 1, max = 2048, message = "name-Name does not match the allowed length;")
    private String name;

    @NotBlank(message = "number-number should not be blank;")
    @Size(min = 1, max = 32, message = "number-number does not match the allowed length;")
    private String number;

    @NotNull(message = "status-Status should not be null;")
    private NomenclatureItemStatus status;

    private List<DefaultAssignmentType> defaultAssignmentType;

}
