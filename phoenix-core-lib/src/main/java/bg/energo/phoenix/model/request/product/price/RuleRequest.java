package bg.energo.phoenix.model.request.product.price;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleRequest {
    private String expression;
    private Map<String, Object> arguments;
}
