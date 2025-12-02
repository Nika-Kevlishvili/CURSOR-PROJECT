package bg.energo.phoenix.model.response.billing.billingRun.condition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConditionPreviewInfo {
    private Long id;
    private String name;
    private String key;

}
