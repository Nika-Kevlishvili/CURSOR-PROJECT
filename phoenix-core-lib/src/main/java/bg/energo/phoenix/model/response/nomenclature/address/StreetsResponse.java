package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.Street;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreetsResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private StreetType type;
    private Boolean defaultSelection;
    private Long orderingId;
    private Long populatedPlaceId;

    public StreetsResponse(Street request) {
        this.id = request.getId();
        this.name = request.getName();
        this.status = request.getStatus();
        this.type = request.getType();
        this.defaultSelection = request.getDefaultSelection();
        this.orderingId = request.getOrderingId();
        this.populatedPlaceId=request.getPopulatedPlace().getId();
    }
}
