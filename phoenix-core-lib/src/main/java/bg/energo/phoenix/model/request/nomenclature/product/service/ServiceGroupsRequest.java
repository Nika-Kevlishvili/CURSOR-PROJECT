package bg.energo.phoenix.model.request.nomenclature.product.service;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceGroupsRequest {
    @NotBlank(message = "name-Name must not be blank")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length, range: [1-512]")
    private String name;

    @NotBlank(message = "nameTransliterated-Name Transliterated must not be blank")
    @Size(min = 1, max = 512, message = "nameTransliterated-Name Transliterated does not match the allowed length, range: [1-512]")
    private String nameTransliterated;

    @NotNull(message = "status-Status must not be null")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default Selection must not be null")
    private Boolean defaultSelection;
}
