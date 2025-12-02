package bg.energo.phoenix.model.request.product.goods.edit;

import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <h1>GoodsSalesAreaEditRequest object</h1>
 * {@link #id}
 * {@link #salesAreaId}
 * {@link #status}
 */
@Data
@AllArgsConstructor
public class GoodsSalesAreaEditRequest {
    private Long id;
    @NotNull(message = "salesAreaId-salesAreaId shouldn't be null;")
    private Long salesAreaId;
    @NotNull(message = "status-GoodsSales status shouldn't be null;")
    private GoodsSubObjectStatus status;
}
