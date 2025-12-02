package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ActivityResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ActivityResponse(Activity activity) {
        this.id = activity.getId();
        this.name = activity.getName();
        this.orderingId = activity.getOrderingId();
        this.defaultSelection = activity.isDefaultSelection();
        this.status = activity.getStatus();
        this.systemUserId = activity.getSystemUserId();
    }
}
