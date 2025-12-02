package bg.energo.phoenix.service.contract.action.calculation.formula;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationContractVariables;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public abstract class PenaltyFormulaVariableEvaluationStrategy {

    public abstract PenaltyFormulaVariable getVariableType();

    public abstract boolean isApplicableToProductContract();

    public abstract boolean isApplicableToServiceContract();

    public abstract Optional<BigDecimal> evaluate(
            PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
            ContractType contractType,
            PenaltyCalculationContractVariables contractVariables,
            List<String> infoErrorMessages
    );

    public static int getScale() {
        return 10;
    }

    public static RoundingMode getRoundingMode() {
        return RoundingMode.HALF_UP;
    }

}
