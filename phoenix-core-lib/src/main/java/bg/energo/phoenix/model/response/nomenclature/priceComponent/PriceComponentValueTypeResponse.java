package bg.energo.phoenix.model.response.nomenclature.priceComponent;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentValueType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class PriceComponentValueTypeResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public PriceComponentValueTypeResponse(PriceComponentValueType valueType){
        this.id = valueType.getId();
        this.name = valueType.getName();
        this.orderingId = valueType.getOrderingId();
        this.defaultSelection = valueType.isDefaultSelection();
        this.status = valueType.getStatus();
        this.systemUserId = valueType.getSystemUserId();
    }
}