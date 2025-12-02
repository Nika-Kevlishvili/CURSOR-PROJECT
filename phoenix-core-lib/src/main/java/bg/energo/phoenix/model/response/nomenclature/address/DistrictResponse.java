package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.District;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class DistrictResponse {
    private Long id;
    private Long populatedPlaceId;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public DistrictResponse(District district) {
        this.id = district.getId();
        this.populatedPlaceId = district.getPopulatedPlace().getId();
        this.name = district.getName();
        this.orderingId = district.getOrderingId();
        this.defaultSelection = district.isDefaultSelection();
        this.status = district.getStatus();
        this.systemUserId = district.getSystemUserId();
    }
}
