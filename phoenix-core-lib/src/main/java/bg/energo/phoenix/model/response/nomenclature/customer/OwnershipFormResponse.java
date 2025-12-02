package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.OwnershipForm;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class OwnershipFormResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public OwnershipFormResponse(OwnershipForm ownershipForm) {
        this.id = ownershipForm.getId();
        this.name = ownershipForm.getName();
        this.orderingId = ownershipForm.getOrderingId();
        this.defaultSelection = ownershipForm.isDefaultSelection();
        this.status = ownershipForm.getStatus();
        this.systemUserId = ownershipForm.getSystemUserId();
    }
}
