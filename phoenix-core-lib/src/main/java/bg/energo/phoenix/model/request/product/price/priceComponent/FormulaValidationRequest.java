package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.customAnotations.product.priceComponent.ValidFormulaVariables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidFormulaVariables
public class FormulaValidationRequest {

    @NotBlank(message = "expression-Expression cannot be null")
    private String expression;

    @Valid
    private List<PriceComponentFormulaVariableRequest> variables;

    @JsonIgnore
    @AssertTrue(message = "formulaRequest.variables-Variables must be unique;")
    public boolean isVariablesUnique() {
        return variables
                .stream()
                .collect(Collectors.groupingBy(PriceComponentFormulaVariableRequest::getVariable, Collectors.counting()))
                .values()
                .stream()
                .noneMatch(count -> count > 1);
    }
}
