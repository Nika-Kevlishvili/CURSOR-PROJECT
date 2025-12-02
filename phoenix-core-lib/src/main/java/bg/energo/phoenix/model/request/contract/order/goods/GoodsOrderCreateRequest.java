package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsOrderCreateRequest extends GoodsOrderBaseRequest {
    @NotNull(message = "orderStatus-Order status is mandatory;")
    private GoodsOrderStatus orderStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "statusModifyDate-Status modify date is mandatory;")
    private LocalDate statusModifyDate;

    @JsonIgnore
    @AssertTrue(message = "orderStatus-Order Status on creation must be REQUESTED or CONFIRMED;")
    public boolean isOrderStatusValid() {
        if (Objects.nonNull(orderStatus)) {
            return List.of(GoodsOrderStatus.REQUESTED, GoodsOrderStatus.CONFIRMED).contains(orderStatus);
        }
        return true;
    }

    @Builder
    public GoodsOrderCreateRequest(GoodsOrderStatus orderStatus, LocalDate statusModifyDate, @Valid GoodsOrderBasicParametersCreateRequest basicParameters, @Valid GoodsOrderGoodsParametersRequest goodsParameters) {
        super(basicParameters, goodsParameters);
        this.orderStatus = orderStatus;
        this.statusModifyDate = statusModifyDate;
    }
}
