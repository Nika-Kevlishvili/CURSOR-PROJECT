package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.GccConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class GccConnectionTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public GccConnectionTypeResponse(GccConnectionType gccConnectionType) {
        this.id = gccConnectionType.getId();
        this.name = gccConnectionType.getName();
        this.orderingId = gccConnectionType.getOrderingId();
        this.defaultSelection = gccConnectionType.isDefaultSelection();
        this.status = gccConnectionType.getStatus();
        this.systemUserId = gccConnectionType.getSystemUserId();
    }
}
