package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSalesAreas;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>GoodsSalesAreaResponse object</h1>
 * {@link #id}
 * {@link #salesArea}
 * {@link #status}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSalesAreaResponse {

    private Long id;
    private SalesAreaResponse salesArea;
    private GoodsSubObjectStatus status;

    public GoodsSalesAreaResponse(GoodsSalesAreas goodsSalesAreas) {
        this.id = goodsSalesAreas.getId();
        this.salesArea = new SalesAreaResponse(goodsSalesAreas.getSalesArea());
        this.status = goodsSalesAreas.getStatus();
    }
}
