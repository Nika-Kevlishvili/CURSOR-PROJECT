package bg.energo.phoenix.model.request.nomenclature.address;

import bg.energo.phoenix.model.customAnotations.nomenclature.NameDefaultValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreetsRequest {

    @NameDefaultValidator
    @NotBlank(message = "name-Name must not be blank;")
    @Length(min = 1,max = 1024,message = "name-Name length should be between {min}:{max} characters;")
    private String name;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "type-Type must not be null;")
    private StreetType type;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;

    @NotNull(message = "populatedPlaceId-Populated place ID must not be null;")
    private Long populatedPlaceId;
}
