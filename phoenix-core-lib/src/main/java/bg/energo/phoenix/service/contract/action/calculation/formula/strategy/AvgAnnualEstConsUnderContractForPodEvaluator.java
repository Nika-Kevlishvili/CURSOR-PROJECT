package bg.energo.phoenix.service.contract.action.calculation.formula.strategy;

import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationContractVariables;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.service.contract.action.calculation.formula.PenaltyFormulaVariableEvaluationStrategy;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvgAnnualEstConsUnderContractForPodEvaluator extends PenaltyFormulaVariableEvaluationStrategy {
    private final ActionTypeProperties actionTypeProperties;

    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.AVERAGE_ANNUAL_ESTIMATED_CONSUMPTION_UNDER_CONTRACT_FOR_POD;
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
    public Optional<BigDecimal> evaluate(PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
                                         ContractType contractType,
                                         PenaltyCalculationContractVariables contractVariables,
                                         List<String> infoErrorMessages) {
        if (!actionTypeProperties.isActionTypeRelatedToPodTermination(formulaEvaluationArguments.getActionTypeId())
            && !formulaEvaluationArguments.getPenaltyApplicability().equals(PenaltyApplicability.POD)) {
            log.error("Unable to calculate average annual estimated consumption under contract for pod: " +
                      "action type is not related to pod termination and penalty applicability is not POD.");
            infoErrorMessages.add(String.format("%s-Unable to calculate average annual estimated consumption under contract for pod: " +
                    "action type is not related to pod termination and penalty applicability is not POD.", INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        if (Objects.isNull(contractVariables.getProductContractVariables().getSummedEstimatedMonthlyAvgConsumptionForPods())) {
            log.error("Unable to calculate summed estimated monthly average consumption for pods.");
            infoErrorMessages.add("%s-Unable to calculate summed estimated monthly average consumption for pods."
                                          .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        try {
            BigDecimal averageAnnualEstimatedConsumptionUnderContractForPod = contractVariables.getProductContractVariables()
                    .getSummedEstimatedMonthlyAvgConsumptionForPods()
                    .multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(1000), getScale(), getRoundingMode());

            log.info("Calculated value for average annual estimated consumption under contract for pod: {}", averageAnnualEstimatedConsumptionUnderContractForPod);

            return Optional.of(averageAnnualEstimatedConsumptionUnderContractForPod);
        } catch (Exception e) {
            log.error("Unable to calculate average annual estimated consumption under contract for pod.", e);
            infoErrorMessages.add("%s-Unable to calculate average annual estimated consumption under contract for pod: exception during arithmetic operation."
                                          .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }
    }

}
