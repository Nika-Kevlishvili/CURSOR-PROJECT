package bg.energo.phoenix.service.billing.runs.models.evaluatePriceComponentCondition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempPriceConditionModel {
    private Long priceComponentId;
    private Long contractDetailId;
    private Long podDetailId;
    private Long podId;

}
