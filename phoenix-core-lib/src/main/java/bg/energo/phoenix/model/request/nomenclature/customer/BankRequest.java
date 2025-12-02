package bg.energo.phoenix.model.request.nomenclature.customer;

import bg.energo.phoenix.model.customAnotations.nomenclature.NameDefaultValidator;
import bg.energo.phoenix.model.customAnotations.nomenclature.ValidBIC;
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
public class BankRequest {
    @NotBlank(message = "name-Name should not be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    @NameDefaultValidator
    private String name;

    @NotBlank(message = "bic-BIC should not be blank;")
    @ValidBIC
    private String bic;

    @NotNull(message = "status-Status should not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection should not be null;")
    private Boolean defaultSelection;
}
