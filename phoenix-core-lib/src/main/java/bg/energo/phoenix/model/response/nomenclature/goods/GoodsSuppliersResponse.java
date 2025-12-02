package bg.energo.phoenix.model.response.nomenclature.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class GoodsSuppliersResponse {
    private Long id;
    private String name;
    private String identifier;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public GoodsSuppliersResponse(GoodsSuppliers goodsSuppliers) {
        this.id = goodsSuppliers.getId();
        this.name = goodsSuppliers.getName();
        this.identifier = goodsSuppliers.getIdentifier();
        this.orderingId = goodsSuppliers.getOrderingId();
        this.defaultSelection = goodsSuppliers.isDefaultSelection();
        this.status = goodsSuppliers.getStatus();
        this.systemUserId = goodsSuppliers.getSystemUserId();
    }
}
