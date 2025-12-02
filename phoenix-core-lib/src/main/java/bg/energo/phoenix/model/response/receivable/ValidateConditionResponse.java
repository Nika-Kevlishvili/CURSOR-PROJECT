package bg.energo.phoenix.model.response.receivable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateConditionResponse {
    private Boolean valid;
}
