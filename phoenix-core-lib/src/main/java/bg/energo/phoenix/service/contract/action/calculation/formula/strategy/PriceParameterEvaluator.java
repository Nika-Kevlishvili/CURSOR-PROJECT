package bg.energo.phoenix.service.contract.action.calculation.formula.strategy;

import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyPriceParameterEvaluationResult;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceParameterEvaluator {

    private final PriceParameterRepository priceParameterRepository;
    private final PriceParameterDetailsRepository priceParameterDetailsRepository;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;

    public List<ActionPenaltyPriceParameterEvaluationResult> evaluate(List<Long> priceParameterIds,
                                                                      List<String> notCalculableVariables,
                                                                      LocalDate executionDate,
                                                                      List<String> infoErrorMessages) {
        log.debug("Evaluating price parameters with ids {}", priceParameterIds);

        LocalDate lastSundayInOctober = executionDate
                .withMonth(Month.OCTOBER.getValue())
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));

        LocalDate lastSundayInMarch = executionDate
                .withMonth(Month.MARCH.getValue())
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));

        List<ActionPenaltyPriceParameterEvaluationResult> priceParameterEvaluationResults = priceParameterRepository
                .findAveragePriceOver30DaysAndPriceParameterIdIn(
                        executionDate,
                        priceParameterIds,
                        lastSundayInOctober,
                        lastSundayInMarch
                );

        if (CollectionUtils.isEmpty(priceParameterEvaluationResults)) {
            for (Long priceParameterId : priceParameterIds) {
                notCalculableVariables.add(PenaltyFormulaVariable.getPriceParameterVariablePrefix() + priceParameterId);
                infoErrorMessages.add("%s-Unable to calculate price parameter with id %s."
                                              .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, priceParameterId));
            }
            return List.of();
        }

        List<Long> evaluatedPriceParameterIds = priceParameterEvaluationResults
                .stream()
                .map(ActionPenaltyPriceParameterEvaluationResult::getId)
                .toList();

        List<String> notCalculablePriceParameters = new ArrayList<>();

        for (Long priceParameterId : priceParameterIds) {
            if (!evaluatedPriceParameterIds.contains(priceParameterId)) {
                notCalculablePriceParameters.add(PenaltyFormulaVariable.getPriceParameterVariablePrefix() + priceParameterId);
                infoErrorMessages.add("%s-Unable to calculate price parameter with id %s."
                                              .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, priceParameterId));
            }
        }

        if (!notCalculablePriceParameters.isEmpty()) {
            notCalculableVariables.addAll(notCalculablePriceParameters);
            return List.of();
        }

        return priceParameterEvaluationResults;
    }

}
