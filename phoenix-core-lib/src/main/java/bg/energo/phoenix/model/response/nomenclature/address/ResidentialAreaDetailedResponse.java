package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.ResidentialArea;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import lombok.Data;

@Data
public class ResidentialAreaDetailedResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private Boolean defaultSelection;
    private String systemUserId;
    private ResidentialAreaType type;
    private Long populatedPlaceId;
    private String populatedPlaceFullName;

    public ResidentialAreaDetailedResponse(ResidentialArea entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.status = entity.getStatus();
        this.orderingId = entity.getOrderingId();
        this.defaultSelection = entity.getDefaultSelection();
        this.systemUserId = entity.getSystemUserId();
        this.type = entity.getType();
        this.populatedPlaceId = entity.getPopulatedPlace().getId();
        this.populatedPlaceFullName = String.join(
                " - ",
                entity.getPopulatedPlace().getName(),
                entity.getPopulatedPlace().getMunicipality().getName(),
                entity.getPopulatedPlace().getMunicipality().getRegion().getName(),
                entity.getPopulatedPlace().getMunicipality().getRegion().getCountry().getName()
        );
    }
}
