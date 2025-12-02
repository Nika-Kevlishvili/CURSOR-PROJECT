package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsOrderEditRequest extends GoodsOrderBaseRequest {

    @NotNull(message = "orderStatus-Order status is mandatory;")
    private GoodsOrderStatus orderStatus;

    @NotNull(message = "statusModifyDate-Status modify date is mandatory;")
    private LocalDate statusModifyDate;

    @Builder
    public GoodsOrderEditRequest(@Valid GoodsOrderBasicParametersCreateRequest basicParameters, @Valid GoodsOrderGoodsParametersRequest goodsParameters, GoodsOrderStatus orderStatus, LocalDate statusModifyDate) {
        super(basicParameters, goodsParameters);
        this.orderStatus = orderStatus;
        this.statusModifyDate = statusModifyDate;
    }
}
