package bg.energo.phoenix.service.contract.action.calculation.formula.strategy;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationContractVariables;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.service.contract.action.calculation.formula.PenaltyFormulaVariableEvaluationStrategy;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Slf4j
@Service
public class TotalEstConsUnderContractEvaluator extends PenaltyFormulaVariableEvaluationStrategy {

    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.TOTAL_ESTIMATED_CONSUMPTION_UNDER_CONTRACT;
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
        BigDecimal estimatedYearlyConsumptionUnderContract = contractVariables.getProductContractVariables().getEstimatedTotalConsumptionUnderContract();

        LocalDate initialTermStartDate = contractVariables.getProductContractVariables().getContractInitialTermStartDate();
        LocalDate contractTermEndDate = contractVariables.getProductContractVariables().getContractTermEndDate();

        if (Objects.isNull(initialTermStartDate)) {
            log.info("Initial term start date is empty in calculation.");
            infoErrorMessages.add("%s-Initial term start date is empty in calculation.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        if (Objects.isNull(contractTermEndDate)) {
            log.error("Contract term end date is empty in calculation.");
            infoErrorMessages.add("%s-Contract term end date is empty in calculation.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        if (Objects.isNull(estimatedYearlyConsumptionUnderContract)) {
            log.error("Unable to calculate estimated yearly consumption under contract.");
            infoErrorMessages.add("%s-Unable to calculate estimated yearly consumption under contract."
                                          .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        try {
            BigDecimal totalEstimatedConsumptionUnderContract = estimatedYearlyConsumptionUnderContract
                    .multiply(BigDecimal.valueOf(EPBDateUtils.calculateMonthsBetween(initialTermStartDate, contractTermEndDate)))
                    .divide(BigDecimal.valueOf(12), getScale(), getRoundingMode());

            log.info("Calculated value for total estimated consumption under contract: {}", totalEstimatedConsumptionUnderContract);

            return Optional.of(totalEstimatedConsumptionUnderContract);
        } catch (Exception e) {
            log.error("Unable to calculate total estimated consumption under contract.", e);
            infoErrorMessages.add("%s-Unable to calculate total estimated consumption under contract: exception during arithmetic operation."
                                          .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }
    }

}
