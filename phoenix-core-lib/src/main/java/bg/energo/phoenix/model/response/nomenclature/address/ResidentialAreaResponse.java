package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.ResidentialArea;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResidentialAreaResponse {

    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private Boolean defaultSelection;
    private String systemUserId;
    private ResidentialAreaType type;
    private Long populatedPlaceId;

    public ResidentialAreaResponse(ResidentialArea entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.status = entity.getStatus();
        this.orderingId = entity.getOrderingId();
        this.defaultSelection = entity.getDefaultSelection();
        this.systemUserId = entity.getSystemUserId();
        this.type = entity.getType();
        this.populatedPlaceId = entity.getPopulatedPlace().getId();
    }
}
