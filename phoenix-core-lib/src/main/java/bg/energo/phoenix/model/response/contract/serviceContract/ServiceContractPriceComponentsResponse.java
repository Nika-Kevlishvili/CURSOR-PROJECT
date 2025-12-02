package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractPriceComponentsResponse {
    private Long formulaVariableId;
    private String variableDescription;
    private BigDecimal values;
}
