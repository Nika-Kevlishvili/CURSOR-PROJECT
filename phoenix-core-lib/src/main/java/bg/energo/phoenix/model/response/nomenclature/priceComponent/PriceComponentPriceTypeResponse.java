package bg.energo.phoenix.model.response.nomenclature.priceComponent;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentPriceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceComponentPriceTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;
    private Boolean isHardCoded;

    public PriceComponentPriceTypeResponse(PriceComponentPriceType priceComponentPriceType) {
        this.id = priceComponentPriceType.getId();
        this.name = priceComponentPriceType.getName();
        this.orderingId = priceComponentPriceType.getOrderingId();
        this.defaultSelection = priceComponentPriceType.getIsDefault();
        this.status = priceComponentPriceType.getStatus();
        this.isHardCoded = priceComponentPriceType.getIsHardcoded();
    }
}
