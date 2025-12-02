package bg.energo.phoenix.model.request.nomenclature.product.goods;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoodsSuppliersRequest {
    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    private String name;

    @Pattern(regexp = "^(\\d+)|(^$)$", message = "identifier-Only digits allowed for identifier, [0:9];")
    @Size(min = 1, max = 32, message = "identifier-Identifier does not match the allowed length, range: [1:32];")
    private String identifier;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default Selection must not be null;")
    private Boolean defaultSelection;
}
