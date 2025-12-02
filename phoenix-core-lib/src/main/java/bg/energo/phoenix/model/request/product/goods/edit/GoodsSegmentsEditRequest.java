package bg.energo.phoenix.model.request.product.goods.edit;

import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <h1>GoodsSegmentsEditRequest</h1>
 * {@link #id}
 * {@link #segmentId}
 * {@link #status}
 */
@Data
@AllArgsConstructor
public class GoodsSegmentsEditRequest {
    private Long id;
    @NotNull(message = "segmentId-segmentId shouldn't be null;")
    private Long segmentId;
    @NotNull(message = "status-status shouldn't be null;")
    private GoodsSubObjectStatus status;
}
