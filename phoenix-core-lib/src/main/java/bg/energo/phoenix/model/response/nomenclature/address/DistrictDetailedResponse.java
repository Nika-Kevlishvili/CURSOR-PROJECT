package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.District;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class DistrictDetailedResponse {
    private Long id;
    private Long populatedPlaceId;
    private String populatedPlaceFullName;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public DistrictDetailedResponse(District district) {
        this.id = district.getId();
        this.populatedPlaceId = district.getPopulatedPlace().getId();
        this.populatedPlaceFullName = String.join(
                " - ",
                district.getPopulatedPlace().getName(),
                district.getPopulatedPlace().getMunicipality().getName(),
                district.getPopulatedPlace().getMunicipality().getRegion().getName(),
                district.getPopulatedPlace().getMunicipality().getRegion().getCountry().getName()
        );
        this.name = district.getName();
        this.orderingId = district.getOrderingId();
        this.defaultSelection = district.isDefaultSelection();
        this.status = district.getStatus();
        this.systemUserId = district.getSystemUserId();
    }
}
