package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.Platform;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class PlatformResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public PlatformResponse(Platform platform) {
        this.id = platform.getId();
        this.name = platform.getName();
        this.orderingId = platform.getOrderingId();
        this.defaultSelection = platform.isDefaultSelection();
        this.status = platform.getStatus();
        this.systemUserId = platform.getSystemUserId();
    }
}
