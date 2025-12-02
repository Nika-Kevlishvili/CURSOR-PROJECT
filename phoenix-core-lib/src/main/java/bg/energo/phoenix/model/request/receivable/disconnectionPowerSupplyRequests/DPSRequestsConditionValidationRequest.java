package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSRequestsConditionValidationRequest {
    @NotBlank(message = "condition-condition cannot be null")
    private String condition;
}
