package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSalesChannels;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>GoodsSalesChannelsResponse object</h1>
 * {@link #id}
 * {@link #salesChannel}
 * {@link #status}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSalesChannelsResponse {
    private Long id;
    private SalesChannelResponse salesChannel;
    private GoodsSubObjectStatus status;

    public GoodsSalesChannelsResponse(GoodsSalesChannels goodsSalesChannels) {
        this.id = goodsSalesChannels.getId();
        this.salesChannel = new SalesChannelResponse(goodsSalesChannels.getSalesChannel());
        this.status = goodsSalesChannels.getStatus();
    }
}
