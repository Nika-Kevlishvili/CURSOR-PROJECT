package bg.energo.phoenix.model.response.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <h1>GoodsViewResponse object</h1>
 * {@link #goodsResponse}
 * {@link #goodsDetailsResponse}
 * {@link #goodsSalesAreaResponses} list of {@link GoodsSalesAreaResponse}
 * {@link #goodsSalesChannelsResponse} list of {@link GoodsSalesChannelsResponse}
 * {@link #goodsSegments} list of {@link GoodsSegmentsResponse}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsViewResponse {

    private GoodsResponse goodsResponse;
    private GoodsDetailsResponse goodsDetailsResponse;
    private List<GoodsSalesAreaResponse> goodsSalesAreaResponses;
    private List<GoodsSalesChannelsResponse> goodsSalesChannelsResponse;
    private List<GoodsSegmentsResponse> goodsSegments;
    private Boolean isLocked;

}
