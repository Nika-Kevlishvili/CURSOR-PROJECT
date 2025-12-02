package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingRunTasksResponse {
    private Long id;
    private Long number;
    private TaskTypeShortResponse taskType;
    private LocalDateTime createDate;
}
