package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PopulatedPlaceResponse {
    private Long id;
    private Long municipalityId;
    private String name;
    private String populatedPlaceName;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public PopulatedPlaceResponse(PopulatedPlace populatedPlace) {
        this.id = populatedPlace.getId();
        this.municipalityId = populatedPlace.getMunicipality().getId();
        this.name = populatedPlace.getName();
        this.orderingId = populatedPlace.getOrderingId();
        this.defaultSelection = populatedPlace.isDefaultSelection();
        this.status = populatedPlace.getStatus();
        this.systemUserId = populatedPlace.getSystemUserId();
    }
}
