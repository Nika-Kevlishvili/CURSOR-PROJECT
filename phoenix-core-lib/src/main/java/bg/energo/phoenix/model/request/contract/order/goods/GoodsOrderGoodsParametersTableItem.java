package bg.energo.phoenix.model.request.contract.order.goods;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GoodsOrderGoodsParametersTableItem {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long goodsDetailId;
    private String name;
    private String codeForConnectionWithOtherSystem;
    private Long goodsUnitId;
    private Integer quantity;
    private BigDecimal price;
    private Long currencyId;
    private String numberOfIncomingAccount;
    private String costCenterOrControllingOrder;
}
