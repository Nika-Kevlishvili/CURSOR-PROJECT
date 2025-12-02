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
public class NegotiatedPavEvaluator extends PenaltyFormulaVariableEvaluationStrategy {

    @Override
    public PenaltyFormulaVariable getVariableType() {
        return PenaltyFormulaVariable.NEGOTIATED_PAV;
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
        BigDecimal negotiatedPav = contractVariables.getProductContractVariables().getAvgHourlyLoadProfiles();
        if (Objects.isNull(negotiatedPav)) {
            log.error("Unable to calculate pav parameter in calculation.");
            infoErrorMessages.add("%s-Unable to calculate pav parameter in calculation."
                                      .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        log.info("Calculated value for negotiated pav: {}", negotiatedPav);

        return Optional.of(negotiatedPav);
    }

}
