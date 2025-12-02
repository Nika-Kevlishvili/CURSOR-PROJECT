package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ServiceGroupsResponse {
    private Long id;
    private String name;
    private String nameTransliterated;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ServiceGroupsResponse(ServiceGroups serviceGroups) {
        this.id = serviceGroups.getId();
        this.name = serviceGroups.getName();
        this.nameTransliterated = serviceGroups.getNameTransliterated();
        this.orderingId = serviceGroups.getOrderingId();
        this.defaultSelection = serviceGroups.isDefaultSelection();
        this.status = serviceGroups.getStatus();
        this.systemUserId = serviceGroups.getSystemUserId();
    }
}
