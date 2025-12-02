package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSegments;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>GoodsSegmentsResponse object</h1>
 * {@link #id}
 * {@link #segment}
 * {@link #status}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSegmentsResponse {

    private Long id;
    private SegmentResponse segment;
    private GoodsSubObjectStatus status;


    public GoodsSegmentsResponse(GoodsSegments goodsSegments) {
        this.id = goodsSegments.getId();
        this.segment = new SegmentResponse(goodsSegments.getSegment());
        this.status = goodsSegments.getStatus();
    }
}
