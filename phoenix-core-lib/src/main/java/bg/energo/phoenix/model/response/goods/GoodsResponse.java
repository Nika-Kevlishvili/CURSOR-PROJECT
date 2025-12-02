package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.enums.product.goods.GoodsStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <h1>GoodsResponse object</h1>
 * {@link #goodsId}
 * {@link #goodsStatusEnum}
 * {@link #systemUserId}
 * {@link #createDate}
 * {@link #modifyDate}
 * {@link #modifySystemUserId}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsResponse {

    private Long goodsId;
    private GoodsStatus goodsStatusEnum;
    private String systemUserId;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;
    private String modifySystemUserId;

    public GoodsResponse(Goods goods) {
        this.goodsId = goods.getId();
        this.goodsStatusEnum = goods.getGoodsStatusEnum();
        this.systemUserId = goods.getSystemUserId();
        this.createDate = goods.getCreateDate();
        this.modifyDate = goods.getModifyDate();
        this.modifySystemUserId = goods.getModifySystemUserId();
    }

}
