package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.RepresentationMethod;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class RepresentationMethodResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public RepresentationMethodResponse(RepresentationMethod representationMethod) {
        this.id = representationMethod.getId();
        this.name = representationMethod.getName();
        this.orderingId = representationMethod.getOrderingId();
        this.defaultSelection = representationMethod.isDefaultSelection();
        this.status = representationMethod.getStatus();
        this.systemUserId = representationMethod.getSystemUserId();
    }
}
