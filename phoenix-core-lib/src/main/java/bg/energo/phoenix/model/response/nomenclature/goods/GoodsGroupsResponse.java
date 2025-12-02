package bg.energo.phoenix.model.response.nomenclature.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class GoodsGroupsResponse {
    private Long id;
    private String name;
    private String nameTransliterated;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public GoodsGroupsResponse(GoodsGroups goodsGroups) {
        this.id = goodsGroups.getId();
        this.name = goodsGroups.getName();
        this.nameTransliterated = goodsGroups.getNameTransliterated();
        this.orderingId = goodsGroups.getOrderingId();
        this.defaultSelection = goodsGroups.isDefaultSelection();
        this.status = goodsGroups.getStatus();
        this.systemUserId = goodsGroups.getSystemUserId();
    }
}
