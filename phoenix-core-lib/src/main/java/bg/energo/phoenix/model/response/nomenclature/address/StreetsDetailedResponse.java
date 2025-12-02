package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.Street;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import lombok.Data;

@Data
public class StreetsDetailedResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private StreetType type;
    private Boolean defaultSelection;
    private Long orderingId;
    private Long populatedPlaceId;
    private String populatedPlaceFullName;

    public StreetsDetailedResponse(Street street) {
        this.id = street.getId();
        this.name = street.getName();
        this.status = street.getStatus();
        this.type = street.getType();
        this.defaultSelection = street.getDefaultSelection();
        this.orderingId = street.getOrderingId();
        this.populatedPlaceId = street.getPopulatedPlace().getId();
        this.populatedPlaceFullName = String.join(
                " - ",
                street.getPopulatedPlace().getName(),
                street.getPopulatedPlace().getMunicipality().getName(),
                street.getPopulatedPlace().getMunicipality().getRegion().getName(),
                street.getPopulatedPlace().getMunicipality().getRegion().getCountry().getName()
        );
    }
}
