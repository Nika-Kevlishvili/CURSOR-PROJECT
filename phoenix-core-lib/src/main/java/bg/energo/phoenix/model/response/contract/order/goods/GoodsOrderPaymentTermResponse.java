package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermDueDateChange;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermType;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderPaymentTermResponse {
    private Long id;
    private String name;
    private GoodsOrderPaymentTermType type;
    private Integer value;
    private CalendarShortResponse calendar;
    private List<GoodsOrderPaymentTermExcludes> excludes;
    private GoodsOrderPaymentTermDueDateChange dueDateChange;
}
