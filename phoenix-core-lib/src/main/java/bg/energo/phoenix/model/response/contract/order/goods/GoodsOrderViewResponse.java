package bg.energo.phoenix.model.response.contract.order.goods;

import lombok.Data;

@Data
public class GoodsOrderViewResponse {
    private GoodsOrderBasicParametersResponse basicParametersResponse;
    private GoodsOrderGoodsParametersResponse goodsParametersResponse;
    private Boolean isLockedByInvoice;
}
