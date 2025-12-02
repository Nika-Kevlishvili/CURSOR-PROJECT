package phoenix.core.customer.model.response.customer;

import lombok.Data;
import phoenix.core.customer.model.entity.nomenclature.address.ZipCode;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

@Data
public class ZipCodeResponse {
    private Long id;
    private Long populatedPlaceId;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ZipCodeResponse(ZipCode zipCode) {
        this.id = zipCode.getId();
        this.populatedPlaceId = zipCode.getPopulatedPlace().getId();
        this.name = zipCode.getName();
        this.orderingId = zipCode.getOrderingId();
        this.defaultSelection = zipCode.isDefaultSelection();
        this.status = zipCode.getStatus();
        this.systemUserId = zipCode.getSystemUserId();
    }
}
