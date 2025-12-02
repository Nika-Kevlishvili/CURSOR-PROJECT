package bg.energo.phoenix.model.customAnotations.contract.service;

import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentProjectionForIap;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ServiceContractPriceComponentFormulaVariables {
    private Long formulaVariableId;
    private PriceComponentMathVariableName variable;
    private String variableDescription;
    private String displayName;
    private BigDecimal value;
    private BigDecimal valueFrom;
    private BigDecimal valueTo;

    public ServiceContractPriceComponentFormulaVariables(PriceComponentProjectionForIap variable) {
        this.formulaVariableId = variable.getId();
        this.variable = variable.getVariableName();
        this.variableDescription = variable.getDescription();
        this.displayName = variable.getDescription() + " (" + variable.getVariableName() + " from " + variable.getPriceComponentName() + ")";
        this.value = variable.getValue();
        this.valueFrom = variable.getValueFrom();
        this.valueTo = variable.getValueTo();
    }
}
