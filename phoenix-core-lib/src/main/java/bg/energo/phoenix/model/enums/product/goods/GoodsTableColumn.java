package bg.energo.phoenix.model.enums.product.goods;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GoodsTableColumn {
    G_ID("id"),
    GD_NAME("goodsDetails.name"),
    GD_GROUP_NAME("goodsDetails.goodsGroups.name"),
    GD_SUPPLIER_NAME("goodsDetails.goodsSuppliers.name"),
    GD_STATUS("goodsDetails.status"),
    GD_PRICE("goodsDetails.price"),
    GD_UNIT("goodsDetails.goodsUnits.name"),
    G_DATE_OF_CREATION("createDate"),
    GD_SALES_CHANNELS("viewGoodsSalesChannels.salesChannelsName");

    private final String value;
}
