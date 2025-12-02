package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.ActionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ActionTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private Boolean isHardCoded;
    private String systemUserId;
    private NomenclatureItemStatus status;

    public ActionTypeResponse(ActionType actionType) {
        this.id = actionType.getId();
        this.name = actionType.getName();
        this.orderingId = actionType.getOrderingId();
        this.defaultSelection = actionType.getDefaultSelection();
        this.status = actionType.getStatus();
        this.systemUserId = actionType.getSystemUserId();
        this.isHardCoded = actionType.getIsHardCoded();
    }

}
