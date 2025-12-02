package bg.energo.phoenix.service.contract.action.calculation.formula.strategy;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationContractVariables;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.service.contract.action.calculation.formula.PenaltyFormulaVariableEvaluationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Service
@Slf4j
public class AvgMonthlyActualConsUnderContractForPodEvaluator extends PenaltyFormulaVariableEvaluationStrategy {
    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.AVERAGE_MONTHLY_ACTUAL_CONSUMPTION_UNDER_CONTRACT_FOR_POD;
    }

    @Override
    public boolean isApplicableToProductContract() {
        return true;
    }

    @Override
    public boolean isApplicableToServiceContract() {
        return false;
    }

    @Override
    public Optional<BigDecimal> evaluate(PenaltyFormulaEvaluationArguments formulaEvaluationArguments, ContractType contractType, PenaltyCalculationContractVariables contractVariables, List<String> infoErrorMessages) {
        if (Objects.isNull(contractVariables.getProductContractVariables().getAvgMonthlyActualConsumptionUnderContractForPod())) {
            log.error("Unable to calculate average monthly actual consumption under contract for pod.");
            infoErrorMessages.add("%s-Unable to calculate average monthly actual consumption under contract for pod."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        log.info("Calculated value for average monthly actual consumption under contract for pod: {}",
                contractVariables.getProductContractVariables().getAvgMonthlyActualConsumptionUnderContractForPod());

        return Optional.of(contractVariables.getProductContractVariables().getAvgMonthlyActualConsumptionUnderContractForPod());
    }
}

