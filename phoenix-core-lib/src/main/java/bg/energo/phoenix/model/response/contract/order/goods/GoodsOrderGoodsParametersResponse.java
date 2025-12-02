package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItemResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderGoodsParametersResponse {
    private String numberOfIncomeAccount;

    private String costCenterOrControllingOrder;

    private Boolean isGlobalVatRate;

    private VatRateResponse vatRate;

    private List<GoodsOrderGoodsParametersTableItemResponse> goods;
}
