package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.ZipCode;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

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
