package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderGoodsParametersTableItemResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;
    private Long goodsDetailId;
    private String name;
    private String codeForConnectionWithOtherSystem;
    private GoodsUnitsShortResponse goodsUnit;
    private Integer quantity;
    private BigDecimal price;
    private CurrencyShortResponse currency;
    private String numberOfIncomingAccount;
    private String costCenterOrControllingOrder;
}
