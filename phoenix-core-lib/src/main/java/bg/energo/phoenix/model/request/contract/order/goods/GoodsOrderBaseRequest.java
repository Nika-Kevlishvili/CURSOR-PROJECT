package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.contract.order.goods.ValidGoodsOrderBankingDetails;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidGoodsOrderBankingDetails
public abstract class GoodsOrderBaseRequest {
    @Valid
    private GoodsOrderBasicParametersCreateRequest basicParameters;

    @Valid
    private GoodsOrderGoodsParametersRequest goodsParameters;
}
