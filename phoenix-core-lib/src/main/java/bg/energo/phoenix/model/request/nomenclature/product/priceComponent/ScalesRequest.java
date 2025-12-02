package bg.energo.phoenix.model.request.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.customAnotations.product.scales.TariffOrScaleValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@TariffOrScaleValidator
public class ScalesRequest {
    @Size(min = 1, max = 512, message = "name-Name is out of range: [{min}:{max}];")
    @NotBlank(message = "name-Name must not be blank;")
    private String name;

    @NotNull(message = "gridOperatorId-Grid Operator Id must not be null;")
    private Long gridOperatorId;

    @Size(min = 1, max = 256, message = "scaleType-Scale Type is out of range: [{min}:{max}];")
    @NotBlank(message = "scaleType-Scale Type must not be blank;")
    private String scaleType;

    @Size(min = 1, max = 256, message = "scaleCode-Scale Code is out of range: [{min}:{max}];")
    private String scaleCode;

    @Size(min = 1, max = 1024, message = "tariffOrScale-Tariff Or Scale is out of range: [{min}:{max}];")
    private String tariffOrScale;

    @NotNull(message = "status-Status can not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-default selection can not be null;")
    private Boolean defaultSelection;

    private Boolean calculationForNumberOfDays;

    private Boolean scaleForActiveElectricity;
}
