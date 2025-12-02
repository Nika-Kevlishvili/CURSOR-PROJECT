package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.CiConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class CiConnectionTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private String systemUserId;
    private NomenclatureItemStatus status;

    public CiConnectionTypeResponse(CiConnectionType ciConnectionType) {
        this.id = ciConnectionType.getId();
        this.name = ciConnectionType.getName();
        this.orderingId = ciConnectionType.getOrderingId();
        this.defaultSelection = ciConnectionType.isDefaultSelection();
        this.systemUserId = ciConnectionType.getSystemUserId();
        this.status = ciConnectionType.getStatus();
    }
}
