package bg.energo.phoenix.model.response.nomenclature.priceComponent;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import lombok.Data;

@Data
public class ScalesResponse {
    private Long id;

    private String name;

    private GridOperatorResponse gridOperator;

    private String scaleType;

    private String scaleCode;

    private String tariffOrScale;

    private boolean defaultSelection;

    private Long orderingId;

    private NomenclatureItemStatus status;

    private Boolean calculationForNumberOfDays;

    private Boolean scaleForActiveElectricity;

    public ScalesResponse(Scales scales) {
        this.id = scales.getId();
        this.name = scales.getName();
        this.gridOperator = new GridOperatorResponse(scales.getGridOperator());
        this.scaleType = scales.getScaleType();
        this.scaleCode = scales.getScaleCode();
        this.tariffOrScale = scales.getTariffScale();
        this.defaultSelection = scales.isDefaultSelection();
        this.orderingId = scales.getOrderingId();
        this.status = scales.getStatus();
        this.calculationForNumberOfDays = scales.getCalculationForNumberOfDays();
        this.scaleForActiveElectricity = scales.getScaleForActiveElectricity();
    }
}
