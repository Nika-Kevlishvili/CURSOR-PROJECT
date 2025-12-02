package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.Region;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class RegionResponse {
    private Long id;
    private Long countryId;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public RegionResponse(Region region) {
        this.id = region.getId();
        this.countryId = region.getCountry().getId();
        this.name = region.getName();
        this.orderingId = region.getOrderingId();
        this.defaultSelection = region.isDefaultSelection();
        this.status = region.getStatus();
        this.systemUserId = region.getSystemUserId();
    }
}
