package bg.energo.phoenix.model.response.contract.productContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractPriceComponentResponse {
    private Long formulaVariableId;
    private String variableDescription;
    private BigDecimal value;
}
