package bg.energo.phoenix.model.request.nomenclature.product.priceComponent;

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
public class PriceComponentValueTypeRequest {
    @NotBlank
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    private String name;
    @NotNull(message = "status-Status can not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-default selection can not be null")
    private Boolean defaultSelection;
}

