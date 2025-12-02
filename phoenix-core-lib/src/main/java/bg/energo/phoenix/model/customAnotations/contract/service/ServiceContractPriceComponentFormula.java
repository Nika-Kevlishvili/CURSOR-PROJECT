package bg.energo.phoenix.model.customAnotations.contract.service;

import lombok.Data;

import java.util.List;

@Data
public class ServiceContractPriceComponentFormula {
    private Long priceComponentId;
    private boolean fromGroup;
    private List<ServiceContractPriceComponentFormulaVariables> variables;
}
