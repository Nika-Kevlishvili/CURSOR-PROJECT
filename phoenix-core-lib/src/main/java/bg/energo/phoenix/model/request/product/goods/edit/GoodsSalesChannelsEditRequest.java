package bg.energo.phoenix.model.request.product.goods.edit;

import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <h1>GoodsSalesChannelsEditRequest object</h1>
 * {@link #id}
 * {@link #salesChannelsId}
 * {@link #status}
 */
@Data
@AllArgsConstructor
public class GoodsSalesChannelsEditRequest {
    private Long id;
    @NotNull(message = "salesChannelsId-salesChannelsId shouldn't be null;")
    private Long salesChannelsId;
    @NotNull(message = "status-status shouldn't be null;")
    private GoodsSubObjectStatus status;
}
