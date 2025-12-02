package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import lombok.Data;

import java.util.List;

@Data
public class PriceComponentFormula {
    private Long priceComponentId;
    private boolean fromGroup;
    private List<PriceComponentFormulaVariables> variables;
}
