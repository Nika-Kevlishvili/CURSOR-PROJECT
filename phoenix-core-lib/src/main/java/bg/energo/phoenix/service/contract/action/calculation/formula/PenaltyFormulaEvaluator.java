package bg.energo.phoenix.service.contract.action.calculation.formula;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyPriceParameterEvaluationResult;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationContractVariables;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationModel;
import bg.energo.phoenix.service.contract.action.calculation.formula.strategy.PriceParameterEvaluator;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy.PriceComponentPrimitivePriceEvaluationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyFormulaEvaluator {

    private final List<PenaltyFormulaVariableEvaluationStrategy> formulaVariableEvaluationStrategies;
    private final PriceParameterEvaluator priceParameterEvaluationStrategy;
    private final PriceComponentPrimitivePriceEvaluationStrategy priceComponentPrimitivePriceEvaluationStrategy;


    public Optional<BigDecimal> evaluateFormulaVariable(PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
                                                        ContractType contractType,
                                                        PenaltyFormulaVariable variable,
                                                        PenaltyCalculationContractVariables contractVariables,
                                                        List<String> informationalErrorMessages) {
        PenaltyFormulaVariableEvaluationStrategy evaluationStrategy = findFormulaVariableEvaluationStrategy(variable);
        if (Objects.isNull(evaluationStrategy)) {
            log.info("No Evaluation strategy found for variable {}", variable);
            informationalErrorMessages.add("%s-No calculation strategy found for variable %s."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, variable));
            return Optional.empty();
        }

        if (!isEvaluationPossible(evaluationStrategy, contractType, contractVariables, informationalErrorMessages)) {
            return Optional.empty();
        }

        return evaluationStrategy.evaluate(formulaEvaluationArguments, contractType, contractVariables, informationalErrorMessages);
    }

    private PenaltyFormulaVariableEvaluationStrategy findFormulaVariableEvaluationStrategy(PenaltyFormulaVariable variable) {
        return formulaVariableEvaluationStrategies
                .stream()
                .filter(strategy -> strategy.getVariableType().equals(variable))
                .findFirst()
                .orElse(null);
    }


    private boolean isEvaluationPossible(PenaltyFormulaVariableEvaluationStrategy evaluationStrategy,
                                         ContractType contractType,
                                         PenaltyCalculationContractVariables contractVariables,
                                         List<String> informationalErrorMessages) {
        switch (contractType) {
            case PRODUCT_CONTRACT -> {
                return isEvaluationPossibleForProductContract(
                        evaluationStrategy,
                        contractVariables,
                        informationalErrorMessages
                );
            }
            case SERVICE_CONTRACT -> {
                return isEvaluationPossibleForServiceContract(
                        evaluationStrategy,
                        contractVariables,
                        informationalErrorMessages
                );
            }
        }

        return false;
    }


    private boolean isEvaluationPossibleForProductContract(PenaltyFormulaVariableEvaluationStrategy evaluationStrategy,
                                                           PenaltyCalculationContractVariables contractVariables,
                                                           List<String> informationalErrorMessages) {
        if (!evaluationStrategy.isApplicableToProductContract()) {
            log.info("Calculation strategy {} is not applicable to product contracts", evaluationStrategy.getClass().getSimpleName());
            informationalErrorMessages.add("%s-Calculation strategy %s is not applicable to product contracts."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, evaluationStrategy.getVariableType().name()));
            return false;
        }

        if (Objects.isNull(contractVariables.getProductContractVariables())) {
            log.info("Unable to calculate because product contract variables are empty.");
            informationalErrorMessages.add("%s-Unable to calculate because product contract variables are empty."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return false;
        }

        return true;
    }


    private boolean isEvaluationPossibleForServiceContract(PenaltyFormulaVariableEvaluationStrategy evaluationStrategy,
                                                           PenaltyCalculationContractVariables contractVariables,
                                                           List<String> informationalErrorMessages) {
        if (!evaluationStrategy.isApplicableToServiceContract()) {
            log.info("Calculation strategy {} is not applicable to service contracts", evaluationStrategy.getClass().getSimpleName());
            informationalErrorMessages.add("%s-Calculation strategy %s is not applicable to service contracts."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, evaluationStrategy.getVariableType().name()));
            return false;
        }

        if (Objects.isNull(contractVariables.getServiceContractVariables())) {
            log.info("Unable to calculate because service contract variables are empty.");
            informationalErrorMessages.add("%s-Unable to calculate because service contract variables are empty."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return false;
        }

        return true;
    }


    public List<ActionPenaltyPriceParameterEvaluationResult> evaluatePriceParameters(List<Long> priceParameterIds,
                                                                                     List<String> notCalculableVariables,
                                                                                     LocalDate executionDate,
                                                                                     List<String> infoErrorMessages) {
        return priceParameterEvaluationStrategy.evaluate(
                priceParameterIds,
                notCalculableVariables,
                executionDate,
                infoErrorMessages
        );
    }


    public Optional<BigDecimal> evaluatePriceComponentTag(Long priceComponentId,
                                                          String priceFormula,
                                                          String tag,
                                                          List<String> infoErrorMessages) {
        BillingDataPriceComponentPriceEvaluationModel evaluationModel =
                new BillingDataPriceComponentPriceEvaluationModel(priceComponentId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        try {
            return Optional.ofNullable(priceComponentPrimitivePriceEvaluationStrategy
                    .evaluate(priceFormula, evaluationModel)
                    .get(0)
                    .price());
        } catch (Exception e) {
            infoErrorMessages.add("%s-Unable to calculate price component with tag %s;"
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, tag));
            log.error("Error occurred during price component evaluation: {}", e.getMessage());
            return Optional.empty();
        }
    }


    /**
     * @return true if penalty formula does not contain multiple consecutive operators and if-else statements,
     * and is a valid math formula, false otherwise.
     */
    public static boolean isValidPenaltyFormula(String formula) {
        log.debug("Validating penalty amount formula: {}", formula);

        if (formula.contains(PenaltyFormulaVariable.getPriceComponentTagVariablePrefix().substring(1))) {

            formula = modifyForTags(formula);

        }

        if (ExpressionStringParser.containsMultipleConsecutiveOperators(formula)) {
            log.info("Formula is invalid: expression contains multiple consecutive operators.");
            return false;
        }

        if (ExpressionStringParser.isIfElseStatement(formula)) {
            log.error("Formula is invalid: expression contains if-else statement.");
            return false;
        }

        boolean isValidMathFormula;
        try {
            isValidMathFormula = ExpressionStringParser.isValidMathFormula(formula);
        } catch (Exception e) {
            log.error("Error while validating penalty amount formula", e);
            return false;
        }

        if (!isValidMathFormula) {
            log.debug("Formula is invalid: expression is not a valid math formula.");
            return false;
        }

        return true;
    }

    private static String modifyForTags(String formula) {
        Pattern pattern = Pattern.compile("\\$(PRICE_COMPONENTS_TAGS_[^$]*)\\$");
        Matcher matcher = pattern.matcher(formula);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tagContent = matcher.group(1);

            String modifiedTagContent = tagContent.replaceAll(" ", "_");

            matcher.appendReplacement(result, Matcher.quoteReplacement("$" + modifiedTagContent + "$"));
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
