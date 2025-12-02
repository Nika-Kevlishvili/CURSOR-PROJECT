package bg.energo.phoenix.model.response.contract.action.calculation.formula;

import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import lombok.Data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PenaltyFormulaVariablesExtractionResult {
    private Set<PenaltyFormulaVariable> formulaVariables;
    private Set<String> priceParameterVariables;
    private Set<String> priceComponentTagVariables;
    private Set<String> invalidVariables;

    public PenaltyFormulaVariablesExtractionResult() {
        this.formulaVariables = new HashSet<>();
        this.priceParameterVariables = new HashSet<>();
        this.priceComponentTagVariables = new HashSet<>();
        this.invalidVariables = new HashSet<>();
    }

    public static PenaltyFormulaVariablesExtractionResult extractVariables(String penaltyFormula) {
        List<String> parsedVariables = ExpressionStringParser.extractVariableNames(penaltyFormula);

        if (parsedVariables.isEmpty()) {
            return new PenaltyFormulaVariablesExtractionResult();
        }

        PenaltyFormulaVariablesExtractionResult result = new PenaltyFormulaVariablesExtractionResult();

        for (String variable : parsedVariables) {
            if (matchesFormulaVariablePattern(variable)) {
                result.addFormulaVariable(variable);
            } else if (matchesPriceParameterVariablePattern(variable)) {
                result.addPriceParameterVariable(variable);
            } else if (matchesPriceComponentTagVariablePattern(variable)) {
                result.addPriceComponentTagVariable(variable);
            } else {
                result.addInvalidVariable(variable);
            }
        }

        return result;
    }

    private void addFormulaVariable(String formulaVariable) {
        this.formulaVariables.add(PenaltyFormulaVariable.valueOf(formulaVariable));
    }

    private void addPriceParameterVariable(String priceParameterVariable) {
        this.priceParameterVariables.add(priceParameterVariable);
    }

    private void addPriceComponentTagVariable(String priceComponentTagVariable) {
        this.priceComponentTagVariables.add(priceComponentTagVariable);
    }

    private void addInvalidVariable(String invalidVariable) {
        this.invalidVariables.add(invalidVariable);
    }

    private static boolean matchesFormulaVariablePattern(String variable) {
        return Arrays.stream(PenaltyFormulaVariable.values()).anyMatch(v -> v.name().matches(variable));
    }

    private static boolean matchesPriceParameterVariablePattern(String variable) {
        return variable.matches(PenaltyFormulaVariable.getPriceParameterVariablePattern());
    }

    private static boolean matchesPriceComponentTagVariablePattern(String variable) {
        return variable.matches(PenaltyFormulaVariable.getPriceComponentTagVariablePattern());
    }

    public boolean containsInvalidVariables() {
        return !this.invalidVariables.isEmpty();
    }

    public boolean isEmpty() {
        return this.formulaVariables.isEmpty()
               && this.priceParameterVariables.isEmpty()
               && this.priceComponentTagVariables.isEmpty();
    }

}
