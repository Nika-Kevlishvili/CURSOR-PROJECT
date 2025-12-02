package bg.energo.phoenix.model.request.billing.billingRun;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingRunConditionValidationRequest {
    @NotBlank(message = "condition-condition cannot be null")
    private String condition;
}
