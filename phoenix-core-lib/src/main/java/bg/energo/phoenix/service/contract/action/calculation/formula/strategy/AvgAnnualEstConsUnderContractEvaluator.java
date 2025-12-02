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

@Slf4j
@Service
public class AvgAnnualEstConsUnderContractEvaluator extends PenaltyFormulaVariableEvaluationStrategy {

    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.AVERAGE_ANNUAL_ESTIMATED_CONSUMPTION_UNDER_CONTRACT;
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
        if (Objects.isNull(contractVariables.getProductContractVariables().getEstimatedTotalConsumptionUnderContract())) {
            log.error("Unable to calculate estimated yearly consumption under contract.");
            infoErrorMessages.add("%s-Unable to calculate estimated yearly consumption under contract."
                                          .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        log.info("Calculated value for average annual estimated consumption under contract: {}",
                 contractVariables.getProductContractVariables().getEstimatedTotalConsumptionUnderContract());

        return Optional.of(contractVariables.getProductContractVariables().getEstimatedTotalConsumptionUnderContract());
    }

}
