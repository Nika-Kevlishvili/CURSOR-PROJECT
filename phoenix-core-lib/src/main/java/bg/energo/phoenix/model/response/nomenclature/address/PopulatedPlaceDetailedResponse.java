package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class PopulatedPlaceDetailedResponse {
    private Long id;
    private Long municipalityId;
    private String municipalityFullName;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public PopulatedPlaceDetailedResponse(PopulatedPlace populatedPlace) {
        this.id = populatedPlace.getId();
        this.municipalityId = populatedPlace.getMunicipality().getId();
        this.municipalityFullName = String.join(
                " - ",
                populatedPlace.getMunicipality().getName(),
                populatedPlace.getMunicipality().getRegion().getName(),
                populatedPlace.getMunicipality().getRegion().getCountry().getName()
        );
        this.name = populatedPlace.getName();
        this.orderingId = populatedPlace.getOrderingId();
        this.defaultSelection = populatedPlace.isDefaultSelection();
        this.status = populatedPlace.getStatus();
        this.systemUserId = populatedPlace.getSystemUserId();
    }
}
