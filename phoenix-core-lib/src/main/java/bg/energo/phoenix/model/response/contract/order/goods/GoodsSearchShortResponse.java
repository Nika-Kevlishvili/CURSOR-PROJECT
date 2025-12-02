package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import lombok.Data;

@Data
public class GoodsSearchShortResponse {
    private Long id;
    private Long goodsDetailVersionId;
    private Long goodsId;
    private String name;

    public GoodsSearchShortResponse(GoodsDetails goodsDetails) {
        this.goodsId = goodsDetails.getGoods().getId();
        this.goodsDetailVersionId = goodsDetails.getVersionId();
        this.id = goodsDetails.getId();
        this.name = "(%s) %s (version: %s)".formatted(goodsDetails.getGoods().getId(), goodsDetails.getName(), goodsDetails.getVersionId());
    }
}
