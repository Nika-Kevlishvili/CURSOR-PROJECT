package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.ElectricityPriceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ElectricityPriceTypeResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ElectricityPriceTypeResponse(ElectricityPriceType priceType){
        this.id = priceType.getId();
        this.name = priceType.getName();
        this.orderingId = priceType.getOrderingId();
        this.defaultSelection = priceType.isDefaultSelection();
        this.status = priceType.getStatus();
        this.systemUserId = priceType.getSystemUserId();
    }

}
