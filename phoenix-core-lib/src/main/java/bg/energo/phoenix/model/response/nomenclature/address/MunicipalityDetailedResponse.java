package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.Municipality;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class MunicipalityDetailedResponse {
    private Long id;
    private Long regionId;
    private String regionFullName;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public MunicipalityDetailedResponse(Municipality municipality) {
        this.id = municipality.getId();
        this.regionId = municipality.getRegion().getId();
        this.regionFullName = String.join(
                " - ",
                municipality.getRegion().getName(),
                municipality.getRegion().getCountry().getName()
        );
        this.name = municipality.getName();
        this.orderingId = municipality.getOrderingId();
        this.defaultSelection = municipality.isDefaultSelection();
        this.status = municipality.getStatus();
        this.systemUserId = municipality.getSystemUserId();
    }
}
