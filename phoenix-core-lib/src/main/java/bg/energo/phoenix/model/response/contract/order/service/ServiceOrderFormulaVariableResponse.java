package bg.energo.phoenix.model.response.contract.order.service;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceOrderFormulaVariableResponse {
    private Long formulaVariableId;
    private String variableDescription;
    private BigDecimal value;
}
