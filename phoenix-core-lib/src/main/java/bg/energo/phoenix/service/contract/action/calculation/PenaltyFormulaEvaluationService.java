package bg.energo.phoenix.service.contract.action.calculation;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.response.contract.action.calculation.*;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaVariablesExtractionResult;
import bg.energo.phoenix.repository.contract.action.ActionPodRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentRepository;
import bg.energo.phoenix.service.contract.action.calculation.formula.PenaltyFormulaEvaluator;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy.AbstractPriceComponentPriceEvaluationStrategy;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBMathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static bg.energo.phoenix.util.epb.EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyFormulaEvaluationService {

    private final ActionRepository actionRepository;
    private final PenaltyRepository penaltyRepository;
    private final ActionPodRepository actionPodRepository;
    private final CurrencyRepository currencyRepository;
    private final PenaltyFormulaEvaluator penaltyFormulaEvaluator;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ProductPriceComponentRepository productPriceComponentRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final ProductPriceComponentGroupRepository productPriceComponentGroupRepository;


    // TODO: 12/11/23 needs to be removed in production
    public boolean testFormulaValidation(String penaltyAmountFormula) {
        return PenaltyFormulaEvaluator.isValidPenaltyFormula(penaltyAmountFormula);
    }


    // TODO: 12/11/23 needs to be removed in production
    public double testFormulaEvaluation(String expression) {
        return EPBMathUtils.evaluateMathExpression(expression);
    }


    @Transactional
    // TODO: 12/12/23 needs to be removed in production
    public void setFormulaToPenalty(Long penaltyId, String formula) {
        if (!PenaltyFormulaEvaluator.isValidPenaltyFormula(formula)) {
            throw new OperationNotAllowedException("Formula is not valid.");
        }

        Penalty penalty = penaltyRepository
                .findByIdAndStatusIn(penaltyId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Penalty not found with id: " + penaltyId));

        penalty.setAmountCalculationFormula(formula);
        penaltyRepository.save(penalty);
    }


    // TODO: 12/11/23 needs to be removed in production
    public String testFormulaCalculationForAction(Long actionId) {
        Action action = actionRepository
                .findByIdAndStatusIn(actionId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found with id: " + actionId));

        if (Objects.isNull(action.getPenaltyId())) {
            throw new OperationNotAllowedException("Action does not have penalty. Unable to calculate.");
        }

        Penalty penalty = penaltyRepository
                .findByIdAndStatusIn(action.getPenaltyId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Penalty not found with id: " + action.getPenaltyId()));

        List<Long> actionPods = actionPodRepository.findPodIdsByActionIdAndStatusIn(actionId, List.of(EntityStatus.ACTIVE));

        List<String> infoErrorMessages = new ArrayList<>();
        PenaltyFormulaEvaluationArguments arguments = PenaltyFormulaEvaluationArguments.fromEntitiesToArguments(action, penalty, actionPods);

        Optional<BigDecimal> calculatedResult = evaluate(arguments, infoErrorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(infoErrorMessages, log);

        if (calculatedResult.isEmpty()) {
            log.error("Unable to calculate penalty amount for action with id: {}", actionId);
            throw new OperationNotAllowedException("Unable to calculate penalty amount for action with id: " + actionId);
        }

        return "Calculated penalty amount: " + calculatedResult.get();
    }


    /**
     * @return {@link Optional} with calculated penalty amount if calculation is successful, otherwise empty.
     */
    public ActionPenaltyCalculationResult tryPenaltyFormulaEvaluation(PenaltyFormulaEvaluationArguments arguments) {
        String formula = arguments.getPenaltyFormula();
        if (StringUtils.isEmpty(formula)) {
            log.debug("Penalty amount formula is empty for penalty with id {}", arguments.getPenaltyId());
            return ActionPenaltyCalculationResult.empty("%s-Penalty amount formula is empty."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        Optional<Long> currencyOptional = getCurrencyForPenaltyCalculation(arguments.getPenaltyCurrencyId());
        if (currencyOptional.isEmpty()) {
            log.error("Currency ID is empty for penalty with id {} and there is no default currency", arguments.getPenaltyId());
            return ActionPenaltyCalculationResult.empty("%s-Currency ID is empty for penalty with id %s and there is no default currency."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, arguments.getPenaltyId()));
        }

        List<String> infoErrorMessages = new ArrayList<>();
        Optional<BigDecimal> evaluationResultOptional = evaluate(arguments, infoErrorMessages);

        if (evaluationResultOptional.isEmpty()) {
            log.info("Unable to calculate penalty amount for penalty with id {}", arguments.getPenaltyId());
            return ActionPenaltyCalculationResult.empty(infoErrorMessages);
        }

        return new ActionPenaltyCalculationResult(
                adjustToPenaltyLimits(evaluationResultOptional.get(), arguments),
                currencyOptional.get(),
                arguments.getPenaltyAutomaticSubmission(),
                infoErrorMessages
        );
    }


    /**
     * @return result adjusted to lower and upper limits if they are present, otherwise returns the same result.
     */
    private static BigDecimal adjustToPenaltyLimits(BigDecimal evaluationResult,
                                                    PenaltyFormulaEvaluationArguments arguments) {
        if (Objects.nonNull(arguments.getPenaltyLowerLimit()) && evaluationResult.compareTo(arguments.getPenaltyLowerLimit()) < 0) {
            log.debug("Amount will be adjusted to lower limit {} as calculated amount {} is less than lower limit", arguments.getPenaltyLowerLimit(), evaluationResult);
            return arguments.getPenaltyLowerLimit();
        }

        if (Objects.nonNull(arguments.getPenaltyUpperLimit()) && evaluationResult.compareTo(arguments.getPenaltyUpperLimit()) > 0) {
            log.debug("Amount will be adjusted to upper limit {} as calculated amount {} is more than upper limit", arguments.getPenaltyUpperLimit(), evaluationResult);
            return arguments.getPenaltyUpperLimit();
        }

        return evaluationResult;
    }


    /**
     * Returns penalty currency ID if it is present, otherwise returns default currency ID.
     * If default currency is not present, returns empty optional.
     */
    public Optional<Long> getCurrencyForPenaltyCalculation(Long penaltyCurrencyId) {
        if (Objects.nonNull(penaltyCurrencyId)) {
            return Optional.of(penaltyCurrencyId);
        }

        Optional<Currency> defaultCurrencyOptional = currencyRepository.findByDefaultSelectionTrue();
        return defaultCurrencyOptional.map(Currency::getId);
    }


    /**
     * @return calculated penalty amount if penalty formula is calculable, empty optional otherwise.
     */
    private Optional<BigDecimal> evaluate(PenaltyFormulaEvaluationArguments arguments,
                                          List<String> infoErrorMessages) {
        log.info("Calculating penalty amount formula: {}", arguments.getPenaltyFormula());

        if (!PenaltyFormulaEvaluator.isValidPenaltyFormula(arguments.getPenaltyFormula())) {
            log.error("Penalty amount formula is not valid. Aborting calculation.");
            infoErrorMessages.add("%s-Penalty amount formula is not valid.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }

        PenaltyFormulaVariablesExtractionResult variablesExtractionResult = PenaltyFormulaVariablesExtractionResult.extractVariables(arguments.getPenaltyFormula());
        if (variablesExtractionResult.containsInvalidVariables()) {
            log.error("Formula is invalid: expression contains invalid variables: {}", variablesExtractionResult.getInvalidVariables());
            infoErrorMessages.add("%s-Formula is invalid: expression contains invalid variables: %s"
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, variablesExtractionResult.getInvalidVariables()));
            return Optional.empty();
        }

        return evaluateExpression(
                arguments,
                infoErrorMessages,
                variablesExtractionResult
        );
    }


    /**
     * @return calculated penalty amount if penalty formula is calculable, empty optional otherwise.
     */
    private Optional<BigDecimal> evaluateExpression(PenaltyFormulaEvaluationArguments arguments,
                                                    List<String> infoErrorMessages,
                                                    PenaltyFormulaVariablesExtractionResult variablesExtractionResult) {
        log.debug("Penalty amount formula contains variables. Calculating expression with variables.");
        List<String> notCalculableVariables = new ArrayList<>();

        Map<String, BigDecimal> variablesMap = evaluateVariableValues(
                arguments,
                notCalculableVariables,
                variablesExtractionResult,
                infoErrorMessages
        );

        if (!notCalculableVariables.isEmpty()) {
            log.error("Penalty amount formula contains variables that cannot be calculated: {}", notCalculableVariables);
            infoErrorMessages.add("%s-Penalty amount formula contains variables that cannot be calculated: %s."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR, String.join(", ", notCalculableVariables)));
            return Optional.empty();
        }

        String expression = replaceVariablesWithCorrespondingValues(arguments.getPenaltyFormula(), variablesMap);

        return evaluateMathExpression(expression, infoErrorMessages);
    }


    /**
     * @return calculated penalty amount if penalty formula is calculable, empty optional otherwise.
     */
    private Optional<BigDecimal> evaluateMathExpression(String penaltyAmountFormula, List<String> informationErrorMessages) {
        log.debug("Penalty amount formula does not contain any variables. Calculating math expression.");
        try {
            BigDecimal value = BigDecimal.valueOf(EPBMathUtils.evaluateMathExpression(penaltyAmountFormula)).setScale(2, RoundingMode.HALF_UP);
            return Optional.of(value);
        } catch (Exception e) {
            log.error("Error while calculating penalty amount formula", e);
            informationErrorMessages.add("%s-Error while evaluating math expression in formula.".formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            return Optional.empty();
        }
    }


    /**
     * @return map of variable names and their corresponding values.
     */
    private Map<String, BigDecimal> evaluateVariableValues(PenaltyFormulaEvaluationArguments arguments,
                                                           List<String> notCalculableVariables,
                                                           PenaltyFormulaVariablesExtractionResult variablesExtractionResult,
                                                           List<String> infoErrorMessages) {
        ContractType contractType = getContractType(arguments.getProductContractId());

        switch (contractType) {
            case PRODUCT_CONTRACT -> {
                return evaluateVariableValuesForProductContract(
                        arguments,
                        notCalculableVariables,
                        variablesExtractionResult,
                        infoErrorMessages
                );
            }
            case SERVICE_CONTRACT -> {
                return evaluateVariableValuesForServiceContract(
                        arguments,
                        notCalculableVariables,
                        variablesExtractionResult,
                        infoErrorMessages
                );
            }
        }

        return new HashMap<>();
    }


    /**
     * @return type of contract. If productContractId is null, returns SERVICE_CONTRACT, otherwise returns PRODUCT_CONTRACT.
     */
    private static ContractType getContractType(Long productContractId) {
        return Objects.isNull(productContractId) ? ContractType.SERVICE_CONTRACT : ContractType.PRODUCT_CONTRACT;
    }


    /**
     * @return map of variable names and their corresponding values for product contract.
     */
    private Map<String, BigDecimal> evaluateVariableValuesForProductContract(PenaltyFormulaEvaluationArguments arguments,
                                                                             List<String> notCalculableVariables,
                                                                             PenaltyFormulaVariablesExtractionResult variables,
                                                                             List<String> infoErrorMessages) {
        log.debug("Populating variable values for product contract with id: {}", arguments.getProductContractId());

        // Collects with one query all information from product contract that will be necessary for variable evaluation.
        // If pods are empty (when action type not related to pod termination), then summed average monthly consumption values
        // will be returned from the contract's respective version's pods.
        PenaltyCalculationProductContractVariables productContractVariables = productContractDetailsRepository
                .getPenaltyCalculationVariablesForProductContract(
                        arguments.getProductContractId(),
                        arguments.getActionExecutionDate(),
                        arguments.getActionTerminationId(),
                        arguments.getActionPods()
                );

        Map<String, BigDecimal> variableValues = new HashMap<>();
        PenaltyCalculationContractVariables contractVariables = new PenaltyCalculationContractVariables(productContractVariables);

        collectFormulaVariableValues(
                arguments,
                notCalculableVariables,
                variables,
                infoErrorMessages,
                ContractType.PRODUCT_CONTRACT,
                contractVariables,
                variableValues
        );

        collectPriceParameterValues(
                notCalculableVariables,
                variables,
                infoErrorMessages,
                arguments.getActionExecutionDate(),
                variableValues
        );

        collectPriceComponentTagValues(
                notCalculableVariables,
                variables,
                infoErrorMessages,
                productContractVariables,
                variableValues,
                arguments
        );

        return variableValues;
    }


    /**
     * Takes care of collecting values for formula variables only.
     */
    private void collectFormulaVariableValues(PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
                                              List<String> notCalculableVariables,
                                              PenaltyFormulaVariablesExtractionResult variables,
                                              List<String> infoErrorMessages,
                                              ContractType contractType,
                                              PenaltyCalculationContractVariables contractVariables,
                                              Map<String, BigDecimal> variableValues) {
        for (PenaltyFormulaVariable formulaVariable : variables.getFormulaVariables()) {
            Optional<BigDecimal> resultOptional = penaltyFormulaEvaluator.evaluateFormulaVariable(
                    formulaEvaluationArguments,
                    contractType,
                    formulaVariable,
                    contractVariables,
                    infoErrorMessages
            );

            if (resultOptional.isEmpty()) {
                log.error("Unable to calculate value for formula variable: {}", formulaVariable);
                notCalculableVariables.add(formulaVariable.name());
                continue;
            }

            variableValues.put(formulaVariable.name(), resultOptional.get());
        }
    }


    /**
     * Takes care of collecting values for price parameters only (allowed for product contracts only).
     */
    private void collectPriceParameterValues(List<String> notCalculableVariables,
                                             PenaltyFormulaVariablesExtractionResult variables,
                                             List<String> infoErrorMessages,
                                             LocalDate executionDate,
                                             Map<String, BigDecimal> variableValues) {
        List<Long> priceParameterIds = new ArrayList<>();

        // A separate tracking list is needed to distinguish between non-calculable price parameter variables
        // and those that are non-calculable due to other variables.
        List<String> notCalculablePriceParameters = new ArrayList<>();

        for (String variable : variables.getPriceParameterVariables()) {
            long priceParameterId;
            try {
                priceParameterId = Long.parseLong(variable.substring(PenaltyFormulaVariable.getPriceParameterVariablePrefix().length()));
                priceParameterIds.add(priceParameterId);
            } catch (NumberFormatException e) {
                log.error("Unable to parse price parameter id from variable: {}", variable);
                notCalculablePriceParameters.add(variable);
            }
        }

        if (!notCalculablePriceParameters.isEmpty()) {
            notCalculableVariables.addAll(notCalculablePriceParameters);
            return;
        }

        // all price parameter variables are queried in one database call to avoid multiple calls
        List<ActionPenaltyPriceParameterEvaluationResult> priceParameterEvaluationResults = penaltyFormulaEvaluator
                .evaluatePriceParameters(
                        priceParameterIds,
                        notCalculableVariables,
                        executionDate,
                        infoErrorMessages
                );

        if (CollectionUtils.isNotEmpty(priceParameterEvaluationResults)) {
            for (ActionPenaltyPriceParameterEvaluationResult priceParameterEvaluationResult : priceParameterEvaluationResults) {
                variableValues.put(
                        PenaltyFormulaVariable.getPriceParameterVariablePrefix() + priceParameterEvaluationResult.getId(),
                        priceParameterEvaluationResult.getAveragePrice()
                );
            }
        }
    }


    /**
     * Takes care of collecting values for price component tags only (allowed for product contracts only).
     */
    private void collectPriceComponentTagValues(List<String> notCalculableVariables,
                                                PenaltyFormulaVariablesExtractionResult variables,
                                                List<String> infoErrorMessages,
                                                PenaltyCalculationProductContractVariables productContractVariables,
                                                Map<String, BigDecimal> variableValues,
                                                PenaltyFormulaEvaluationArguments arguments) {
        for (String variable : variables.getPriceComponentTagVariables()) {
            String priceComponentTag = variable.substring(PenaltyFormulaVariable.getPriceComponentTagVariablePrefix().length() - 1);
            Long productDetailId = productContractDetailsRepository.getProductDetailIdByContractDetailId(productContractVariables.getContractDetailId());
            List<PriceComponentFormulaDto> primitivePriceComponents = new ArrayList<>();
            filterAllConnectedPriceComponents(
                    productDetailId,
                    priceComponentTag,
                    arguments.getActionExecutionDate(),
                    primitivePriceComponents);

            if (CollectionUtils.isEmpty(primitivePriceComponents)) {
                infoErrorMessages.add("%s-Unable to calculate there is not valid price component for used price component tag in calculation;"
                        .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            } else if (primitivePriceComponents.size() > 1) {
                infoErrorMessages.add("%s-Unable to calculate There is more then 1 price component for used price component tag in calculation;"
                        .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            } else {
                Optional<BigDecimal> resultOptional = penaltyFormulaEvaluator.evaluatePriceComponentTag(
                        primitivePriceComponents.get(0).getId(),
                        primitivePriceComponents.get(0).getFormula(),
                        priceComponentTag,
                        infoErrorMessages);
                if (resultOptional.isEmpty()) {
                    log.error("Unable to calculate value for variable: {}", variable);
                    notCalculableVariables.add(variable);
                    continue;
                }
                variableValues.put(variable, resultOptional.get());
            }
        }
    }

    private void filterAllConnectedPriceComponents(Long productDetailId,
                                                   String tag,
                                                   LocalDate actionExecutionDate,
                                                   List<PriceComponentFormulaDto> primitivePriceComponents) {
        Set<Long> connectedPriceComponentIds = new HashSet<>();
        List<Long> priceComponentIdsFromGroups = productPriceComponentGroupRepository
                .getPriceComponentIdsFromRespectiveGroups(actionExecutionDate, tag, productDetailId);
        List<Long> priceComponentIds = productPriceComponentRepository.getActivePriceComponentIdsByProductDetailId(productDetailId);
        connectedPriceComponentIds.addAll(Set.copyOf(priceComponentIdsFromGroups));
        connectedPriceComponentIds.addAll(Set.copyOf(priceComponentIds));
        List<PriceComponentFormulaDto> priceComponents = priceComponentRepository
                .getByTagWithoutPriceProfileFormulaAndIdIn(connectedPriceComponentIds, tag);
        primitivePriceComponents.addAll(priceComponents
                .stream()
                .filter(pc -> AbstractPriceComponentPriceEvaluationStrategy
                        .definePriceComponentFormulaComplexity(pc.getFormula())
                        .equals(PriceComponentFormulaComplexity.PRIMITIVE))
                .toList());
    }


    /**
     * @return map of variable names and their corresponding values for service contract.
     */
    private Map<String, BigDecimal> evaluateVariableValuesForServiceContract(PenaltyFormulaEvaluationArguments arguments,
                                                                             List<String> notCalculableVariables,
                                                                             PenaltyFormulaVariablesExtractionResult variables,
                                                                             List<String> infoErrorMessages) {
        log.debug("Populating variable values for service contract with id: {}", arguments.getServiceContractId());

        PenaltyCalculationServiceContractVariables serviceContractVariables = serviceContractDetailsRepository
                .getPenaltyCalculationVariablesForServiceContract(
                        arguments.getServiceContractId(),
                        arguments.getActionExecutionDate(),
                        arguments.getActionTerminationId()
                );

        if (CollectionUtils.isNotEmpty(variables.getPriceParameterVariables())) {
            log.error("Price parameter value calculation is not supported by service contracts.");
            infoErrorMessages.add("%s-Price parameter value calculation is not supported by service contracts."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        if (CollectionUtils.isNotEmpty(variables.getPriceComponentTagVariables())) {
            log.error("Price component tag value calculation is not supported by service contracts.");
            infoErrorMessages.add("%s-Price component tag value calculation is not supported by service contracts."
                    .formatted(INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        Map<String, BigDecimal> variableValues = new HashMap<>();

        collectFormulaVariableValues(
                arguments,
                notCalculableVariables,
                variables,
                infoErrorMessages,
                ContractType.SERVICE_CONTRACT,
                new PenaltyCalculationContractVariables(serviceContractVariables),
                variableValues
        );

        return variableValues;
    }


    /**
     * Replaces variables in expression with corresponding values.
     */
    private String replaceVariablesWithCorrespondingValues(String expression, Map<String, BigDecimal> variablesMap) {
        log.debug("Replacing variables with corresponding values in expression: {}", expression);

        for (String variable : variablesMap.keySet()) {
            String variablePlaceholder = "$" + variable + "$";
            BigDecimal variableValue = variablesMap.get(variable);

            if (expression.contains(variablePlaceholder)) {
                expression = expression.replace(variablePlaceholder, variableValue.toString());
            }
        }

        return expression;
    }

}
