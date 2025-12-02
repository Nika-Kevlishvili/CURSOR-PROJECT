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
public class DaysBetweenRealTerminationAndTermEndDateEvaluator extends PenaltyFormulaVariableEvaluationStrategy {

    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.NUMBER_OF_DAYS_FROM_REAL_TERMINATION_DATE_TO_END_DATE_OF_CONTRACT_TERM;
    }

    @Override
    public boolean isApplicableToProductContract() {
        return true;
    }

    @Override
    public boolean isApplicableToServiceContract() {
        return true;
    }

    @Override
    public Optional<BigDecimal> evaluate(PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
                                         ContractType contractType,
                                         PenaltyCalculationContractVariables contractVariables,
                                         List<String> infoErrorMessages) {
        switch (contractType) {
            case PRODUCT_CONTRACT -> {
                return calculateDaysBetweenRealTerminationAndContractTermEndDates(
                        contractVariables.getProductContractVariables().getRealTerminationDate(),
                        contractVariables.getProductContractVariables().getContractTermEndDate(),
                        infoErrorMessages
                );
            }
            case SERVICE_CONTRACT -> {
                return calculateDaysBetweenRealTerminationAndContractTermEndDates(
                        contractVariables.getServiceContractVariables().getRealTerminationDate(),
                        contractVariables.getServiceContractVariables().getContractTermEndDate(),
                        infoErrorMessages
                );
            }
        }

        return Optional.empty();
    }

    private static Optional<BigDecimal> calculateDaysBetweenRealTerminationAndContractTermEndDates(LocalDate realTerminationDate,
                                                                                                   LocalDate contractTermEndDate,
                                                                                                   List<String> infoErrorMessages) {
        if (Objects.isNull(contractTermEndDate)) {
            log.error("Initial term end date is empty in calculation.");
            infoErrorMessages.add("%s-Initial term end date is empty in calculation.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        if (Objects.isNull(realTerminationDate)) {
            log.error("Real termination date is empty in calculation.");
            infoErrorMessages.add("%s-Real termination date is empty in calculation.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        BigDecimal daysBetween = BigDecimal.valueOf(EPBDateUtils.calculateDaysBetween(realTerminationDate, contractTermEndDate));

        log.info("Calculated value for number of days from real termination date to end date of contract term: {}", daysBetween);

        return Optional.of(daysBetween);
    }

}
