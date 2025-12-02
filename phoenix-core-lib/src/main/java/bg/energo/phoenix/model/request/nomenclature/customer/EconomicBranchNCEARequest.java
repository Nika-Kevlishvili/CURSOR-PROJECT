package bg.energo.phoenix.model.request.nomenclature.customer;

import bg.energo.phoenix.model.customAnotations.nomenclature.NameDefaultValidator;
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
public class EconomicBranchNCEARequest {

    @NameDefaultValidator
    @NotBlank(message = "name-Name must not be blank;")
    @Length(min = 1, max = 1024,message = "name-Name length should be between {min}:{max} characters;")
    private String name;

    @NotNull(message = "status-Status can not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection can not be null;")
    private Boolean defaultSelection;

}
