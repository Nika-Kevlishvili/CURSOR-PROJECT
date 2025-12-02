package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.customAnotations.nomenclature.ValidSubActivityJsonFields;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidSubActivityJsonFields
public class SubActivityRequest {

    @NotBlank(message = "name-Name should not be Blank;")
    @Length(min = 1, max = 512, message = "name-Name length should be between {min} and {max};")
    private String name;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;

    private Long activityId;

    @NotEmpty(message = "fields-Fields should not be empty;")
    private List<@Valid SubActivityJsonField> fields;

}
