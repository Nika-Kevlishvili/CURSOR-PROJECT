package bg.energo.phoenix.model.response.nomenclature.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import lombok.Data;

@Data
public class GoodsUnitsShortResponse {
    private Long id;
    private String name;

    public GoodsUnitsShortResponse(GoodsUnits goodsUnits) {
        this.id = goodsUnits.getId();
        this.name = goodsUnits.getName();
    }
}
