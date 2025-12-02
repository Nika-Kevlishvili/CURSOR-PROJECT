package bg.energo.phoenix.model.response.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class MeasurementTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;
    private Long gridOperatorId;
    private String gridOperatorName;

    public MeasurementTypeResponse(MeasurementType measurementType,String gridOperatorName) {
        this.id = measurementType.getId();
        this.name = measurementType.getName();
        this.orderingId = measurementType.getOrderingId();
        this.defaultSelection = measurementType.isDefault();
        this.status = measurementType.getStatus();
        this.gridOperatorId = measurementType.getGridOperatorId();
        this.gridOperatorName = gridOperatorName;
    }
}
