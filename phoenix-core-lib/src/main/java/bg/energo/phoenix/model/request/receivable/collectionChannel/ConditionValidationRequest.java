package bg.energo.phoenix.model.request.receivable.collectionChannel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConditionValidationRequest {
    @NotBlank(message = "condition-condition cannot be null")
    private String condition;
}
