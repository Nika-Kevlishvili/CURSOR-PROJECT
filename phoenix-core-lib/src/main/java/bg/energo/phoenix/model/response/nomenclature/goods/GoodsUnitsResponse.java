package bg.energo.phoenix.model.response.nomenclature.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoodsUnitsResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;

    public GoodsUnitsResponse(GoodsUnits goodsUnits){
        this.id = goodsUnits.getId();
        this.name = goodsUnits.getName();
        this.orderingId = goodsUnits.getOrderingId();
        this.defaultSelection = goodsUnits.getIsDefault();
        this.status = goodsUnits.getStatus();
    }
}
