package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ServiceTypeResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ServiceTypeResponse(ServiceType serviceType){
        this.id = serviceType.getId();
        this.name = serviceType.getName();
        this.orderingId = serviceType.getOrderingId();
        this.defaultSelection = serviceType.isDefaultSelection();
        this.status = serviceType.getStatus();
        this.systemUserId = serviceType.getSystemUserId();
    }
}
