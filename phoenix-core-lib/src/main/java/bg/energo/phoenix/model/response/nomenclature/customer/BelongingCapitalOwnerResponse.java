package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.BelongingCapitalOwner;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class BelongingCapitalOwnerResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public BelongingCapitalOwnerResponse(BelongingCapitalOwner belongingCapitalOwner) {
        this.id = belongingCapitalOwner.getId();
        this.name = belongingCapitalOwner.getName();
        this.orderingId = belongingCapitalOwner.getOrderingId();
        this.defaultSelection = belongingCapitalOwner.isDefaultSelection();
        this.status = belongingCapitalOwner.getStatus();
        this.systemUserId = belongingCapitalOwner.getSystemUserId();
    }
}
