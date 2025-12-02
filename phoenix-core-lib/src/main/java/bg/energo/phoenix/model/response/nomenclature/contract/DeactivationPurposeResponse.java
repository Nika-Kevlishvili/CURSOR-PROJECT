package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.DeactivationPurpose;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeactivationPurposeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private Boolean isHardCoded;
    private NomenclatureItemStatus status;

    public DeactivationPurposeResponse(DeactivationPurpose deactivationPurpose) {
        this.id = deactivationPurpose.getId();
        this.name = deactivationPurpose.getName();
        this.orderingId = deactivationPurpose.getOrderingId();
        this.defaultSelection = deactivationPurpose.getIsDefault();
        this.isHardCoded = deactivationPurpose.getIsHardCoded();
        this.status = deactivationPurpose.getStatus();
    }
}
