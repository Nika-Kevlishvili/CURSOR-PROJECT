package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.contract.order.goods.request.GoodsOrderGoodsParametersTableValidator;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GoodsOrderGoodsParametersRequest {
    @Size(min = 1, max = 32, message = "goodsParameters.numberOfIncomeAccount-Number of Income Account invalid size, valid size: min [{min}] and max [{max}]")
    private String numberOfIncomeAccount;

    @Size(min = 1, max = 32, message = "goodsParameters.costCenterOrControllingOrder-Cost center or Controlling order invalid size, valid size: min [{min}] and max [{max}]")
    private String costCenterOrControllingOrder;

    private boolean isGlobalVatRate;

    private Long vatRateId;

    @GoodsOrderGoodsParametersTableValidator
    private List<GoodsOrderGoodsParametersTableItem> goods;
}
