package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.contract.order.goods.ValidGoodsOrderPaymentTermValues;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermDueDateChange;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@ValidGoodsOrderPaymentTermValues
public class GoodsOrderPaymentTermRequest {
    private Long id;
    @NotBlank(message = "basicParameters.paymentTerm.name-Name must not be blank;")
    private String name;
    @NotNull(message = "basicParameters.paymentTerm.type-Type must not be null;")
    private GoodsOrderPaymentTermType type;
    private Integer value;
    @NotNull(message = "basicParameters.paymentTerm.calendarId-Calendar must not be null;")
    private Long calendarId;
    private List<GoodsOrderPaymentTermExcludes> excludes;
    private GoodsOrderPaymentTermDueDateChange dueDateChange;
}
