package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderProxyFilesResponse {
    private Long id;
    private String name;
    private String fileUrl;
    private Long goodsOrderProxyId;
    private EntityStatus status;
}
