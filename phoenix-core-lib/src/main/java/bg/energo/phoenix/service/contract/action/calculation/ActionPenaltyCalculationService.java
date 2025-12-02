package bg.energo.phoenix.service.contract.action.calculation;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResponse;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResult;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaEvaluationArguments;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyTransformationByApplicabilityArguments;
import bg.energo.phoenix.repository.contract.action.ActionPodRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.service.contract.action.document.ActionDocumentCreationService;
import bg.energo.phoenix.service.contract.action.liability.ActionLiabilityGenerationService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer.EPRES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionPenaltyCalculationService {

    private final ActionRepository actionRepository;
    private final ActionPodRepository actionPodRepository;
    private final PenaltyRepository penaltyRepository;
    private final ActionTypeProperties actionTypeProperties;
    private final ActionLiabilityGenerationService liabilityGenerationService;
    private final PenaltyFormulaEvaluationService penaltyFormulaEvaluationService;
    private final ActionDocumentCreationService actionDocumentCreationService;
    private final CustomerLiabilityService customerLiabilityService;

    /**
     * Calculates penalty amount for the action if all the information is present and valid.
     * Penalty amount calculation is a part of action creation and update flows (except when action locked due to generated liability).
     *
     * @param request action request with all the information needed for penalty amount calculation
     * @return {@link ActionPenaltyCalculationResult} with calculated penalty amount and currency id
     */
    public ActionPenaltyCalculationResult calculatePenaltyAmount(ActionRequest request, Long actionIdToExclude) {
        log.debug("Calculating penalty for action request: {}", request);

        if (Objects.isNull(request.getPenaltyId())) {
            return ActionPenaltyCalculationResult.empty("%s-Penalty not selected in action. Unable to calculate."
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        Optional<Penalty> penaltyOptional = penaltyRepository.findByIdAndStatus(request.getPenaltyId(), EntityStatus.ACTIVE);
        if (penaltyOptional.isEmpty()) {
            log.error("Penalty with id {} not found", request.getPenaltyId());
            return ActionPenaltyCalculationResult.empty("%s-Penalty not found with ID %s and status %s."
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR, request.getPenaltyId(), EntityStatus.ACTIVE));
        }

        PenaltyFormulaEvaluationArguments arguments = PenaltyFormulaEvaluationArguments.fromRequestToArguments(request, penaltyOptional.get());
        ActionPenaltyCalculationResult initialCalculationResult = penaltyFormulaEvaluationService.tryPenaltyFormulaEvaluation(arguments);

        if (initialCalculationResult.isEmpty()) {
            log.debug("Penalty amount is empty or not calculable for penalty with id {}", penaltyOptional.get().getId());
            return initialCalculationResult;
        }

        PenaltyTransformationByApplicabilityArguments transformationByApplicabilityArguments = PenaltyTransformationByApplicabilityArguments
                .fromEvaluationArguments(
                        arguments,
                        initialCalculationResult,
                        actionIdToExclude
                );

        return processCalculatedAmountByPenaltyApplicability(transformationByApplicabilityArguments);
    }


    /**
     * Returns calculated penalty amount processed according to penalty applicability, action's type and other persisted actions.
     */
    private ActionPenaltyCalculationResult processCalculatedAmountByPenaltyApplicability(PenaltyTransformationByApplicabilityArguments arguments) {
        log.debug("Processing calculated penalty amount {} by penalty applicability {}",
                arguments.getFormulaEvaluationResult().amount(), arguments.getPenaltyApplicability());

        switch (arguments.getPenaltyApplicability()) {
            case EVENT -> {
                return arguments.getFormulaEvaluationResult();
            }
            case CONTRACT -> {
                return getCalculationResultWhenPenaltyApplicabilityIsContract(arguments);
            }
            case POD -> {
                return getCalculationResultWhenPenaltyApplicabilityIsPod(arguments);
            }
        }

        return ActionPenaltyCalculationResult.empty(
                arguments.getFormulaEvaluationResult().infoErrorMessages(),
                "%s-Penalty applicability %s not supported".formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR, arguments.getPenaltyApplicability())
        );
    }


    /**
     * Returns calculated penalty amount and currency if penalty applicability is contract and there is
     * no other action with same contract and action type and penalty applicability, empty result otherwise.
     */
    private ActionPenaltyCalculationResult getCalculationResultWhenPenaltyApplicabilityIsContract(PenaltyTransformationByApplicabilityArguments arguments) {
        log.debug("Calculating penalty amount when penalty applicability is contract");

        return CollectionUtils.isNotEmpty(findPenaltyClaimedActionsWithSameContractAndActionTypeAndPenaltyApplicability(arguments))
                ? ActionPenaltyCalculationResult.empty(arguments.getFormulaEvaluationResult().infoErrorMessages())
                : arguments.getFormulaEvaluationResult();
    }


    /**
     * Returns calculated penalty amount and currency result based on action type and pod coverage.
     */
    private ActionPenaltyCalculationResult getCalculationResultWhenPenaltyApplicabilityIsPod(PenaltyTransformationByApplicabilityArguments arguments) {
        log.debug("Calculating penalty amount when penalty applicability is pod");

        List<Long> claimedActionsWithSameContractAndTypeAndApplicability = findPenaltyClaimedActionsWithSameContractAndActionTypeAndPenaltyApplicability(arguments);

        if (actionTypeProperties.isActionTypeRelatedToPodTermination(arguments.getActionTypeId())) {
            return getCalculationResultWhenApplicabilityPodAndTypeRelatedToPodTermination(
                    arguments,
                    claimedActionsWithSameContractAndTypeAndApplicability
            );
        } else {
            return getCalculationResultWhenApplicabilityPodAndTypeNotRelatedToPodTermination(
                    arguments,
                    claimedActionsWithSameContractAndTypeAndApplicability
            );
        }
    }


    /**
     * Returns calculated penalty amount and currency if penalty applicability is pod and action type is related to pod termination (with/without notice).
     * If there are other actions with same contract and action type and penalty applicability, the result is calculated
     * based on the pods coverage of these actions, empty result otherwise. If not, if formula contains POD-related variables,
     * the calculation result is returned, otherwise the result is calculated based on the pods coverage of the current action.
     */
    private ActionPenaltyCalculationResult getCalculationResultWhenApplicabilityPodAndTypeRelatedToPodTermination(PenaltyTransformationByApplicabilityArguments arguments,
                                                                                                                  List<Long> claimedActionsWithSameContractAndTypeAndApplicability) {
        log.debug("Calculating penalty amount when penalty applicability is pod and action type is related to pod termination");

        if (CollectionUtils.isNotEmpty(claimedActionsWithSameContractAndTypeAndApplicability)) {
            Long podsNotCoveredByPersistedActions = actionPodRepository.countPodsNotCoveredByPersistedActions(
                    CollectionUtils.emptyIfNull(arguments.getActionPods())
                            .stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")),
                    claimedActionsWithSameContractAndTypeAndApplicability
            );

            if (podsNotCoveredByPersistedActions == 0) { // if all pods were covered by these actions
                return ActionPenaltyCalculationResult.empty(arguments.getFormulaEvaluationResult().infoErrorMessages());
            } else {
                if (penaltyFormulaDependsOnPods(arguments.getPenaltyFormula())) {
                    return arguments.getFormulaEvaluationResult();
                } else {
                    // set amount multiplied by the difference between the pods covered and the pods from the current action
                    return new ActionPenaltyCalculationResult(
                            arguments.getFormulaEvaluationResult().amount().multiply(BigDecimal.valueOf(podsNotCoveredByPersistedActions)),
                            arguments.getFormulaEvaluationResult().currencyId(),
                            arguments.getPenaltyAutomaticSubmission(),
                            arguments.getFormulaEvaluationResult().infoErrorMessages()
                    );
                }
            }
        } else {
            if (penaltyFormulaDependsOnPods(arguments.getPenaltyFormula())) {
                return arguments.getFormulaEvaluationResult();
            } else {
                return new ActionPenaltyCalculationResult(
                        arguments.getFormulaEvaluationResult().amount().multiply(BigDecimal.valueOf(arguments.getActionPods().size())),
                        arguments.getFormulaEvaluationResult().currencyId(),
                        arguments.getPenaltyAutomaticSubmission(),
                        arguments.getFormulaEvaluationResult().infoErrorMessages()
                );
            }
        }
    }


    /**
     * Returns calculated penalty amount and currency if penalty applicability is pod and action type is not related to pod termination (any other).
     * If there are other actions with same contract and action type and penalty applicability, the result is calculated
     * based on the pods coverage of these actions, empty result otherwise. If not, if formula contains POD-related variables,
     * the calculation result is returned, otherwise the result is calculated based on the pods coverage of the current action.
     * In this case, as non pod related action types don't allow pods attachment to the action, the pods needed for calculation
     * are considered from the contract's respective and its future versions.
     * If contract type is service, the result is always empty, as service contracts don't allow pods attachment to the action.
     */
    private ActionPenaltyCalculationResult getCalculationResultWhenApplicabilityPodAndTypeNotRelatedToPodTermination(PenaltyTransformationByApplicabilityArguments arguments,
                                                                                                                     List<Long> claimedActionsWithSameContractAndTypeAndApplicability) {
        log.debug("Calculating penalty amount when penalty applicability is pod and action type is not related to pod termination");

        if (arguments.getContractType().equals(ContractType.SERVICE_CONTRACT)) {
            return ActionPenaltyCalculationResult.empty(arguments.getFormulaEvaluationResult().infoErrorMessages());
        }

        if (CollectionUtils.isNotEmpty(claimedActionsWithSameContractAndTypeAndApplicability)) {
            Long podsNotCoveredByPersistedActions = actionPodRepository.countPodsNotCoveredByRespectiveAndFutureContractVersionsOfPersistedActions(
                    arguments.getContractId(),
                    arguments.getExecutionDate(),
                    claimedActionsWithSameContractAndTypeAndApplicability
            );

            if (podsNotCoveredByPersistedActions == 0) { // all pods are covered by these actions
                return ActionPenaltyCalculationResult.empty(arguments.getFormulaEvaluationResult().infoErrorMessages());
            } else {
                if (penaltyFormulaDependsOnPods(arguments.getPenaltyFormula())) {
                    return arguments.getFormulaEvaluationResult();
                } else {
                    // set amount multiplied by the difference between the pods covered and the pods from the contract's respective and future versions
                    return new ActionPenaltyCalculationResult(
                            arguments.getFormulaEvaluationResult().amount().multiply(BigDecimal.valueOf(podsNotCoveredByPersistedActions)),
                            arguments.getFormulaEvaluationResult().currencyId(),
                            arguments.getPenaltyAutomaticSubmission(),
                            arguments.getFormulaEvaluationResult().infoErrorMessages()
                    );
                }
            }
        } else {
            if (penaltyFormulaDependsOnPods(arguments.getPenaltyFormula())) {
                return arguments.getFormulaEvaluationResult();
            } else {
                Long podsFromRespectiveAndFutureContractVersions = actionPodRepository.countPodsFromRespectiveAndFutureContractVersions(arguments.getContractId(), arguments.getExecutionDate());
                return new ActionPenaltyCalculationResult(
                        arguments.getFormulaEvaluationResult().amount().multiply(BigDecimal.valueOf(podsFromRespectiveAndFutureContractVersions)),
                        arguments.getFormulaEvaluationResult().currencyId(),
                        arguments.getPenaltyAutomaticSubmission(),
                        arguments.getFormulaEvaluationResult().infoErrorMessages()
                );
            }
        }
    }


    /**
     * @return list of action IDs if actions with same contract and action type and penalty applicability exists and has penalty claimed
     */
    private List<Long> findPenaltyClaimedActionsWithSameContractAndActionTypeAndPenaltyApplicability(PenaltyTransformationByApplicabilityArguments arguments) {
        switch (arguments.getContractType()) {
            case PRODUCT_CONTRACT -> {
                return actionRepository.findPenaltyClaimedActionsWithProductContractAndActionTypeAndPenaltyApplicability(
                        arguments.getContractId(),
                        arguments.getActionTypeId(),
                        arguments.getPenaltyApplicability(),
                        arguments.getActionIdToExclude()
                );
            }
            case SERVICE_CONTRACT -> {
                return actionRepository.findPenaltyClaimedActionsWithServiceContractAndActionTypeAndPenaltyApplicability(
                        arguments.getContractId(),
                        arguments.getActionTypeId(),
                        arguments.getPenaltyApplicability(),
                        arguments.getActionIdToExclude()
                );
            }
        }

        return List.of();
    }


    /**
     * @return true if formula variables needed for penalty amount calculation depend on action PODs
     */
    private boolean penaltyFormulaDependsOnPods(String penaltyCalculationFormula) {
        // TODO: 11/22/23 possible impl - if the formula contains POD-related variables (the list has to be confirmed)

        for (PenaltyFormulaVariable variable : PenaltyFormulaVariable.getVariablesRelatedToPods()) {
            if (penaltyCalculationFormula.contains(variable.name())) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }


    /**
     * Calculates penalty amount (if possible) for all eligible non-claimed actions suitable for penalty calculation scheduled job.
     */
    public void calculatePenaltiesForEligibleActions(int numberOfThreads, int queryBatchSize, int batchSize) {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        int page = 0;
        List<Callable<Boolean>> callables = new ArrayList<>();

        while (true) {
            Page<ActionPenaltyCalculationResponse> eligibleActions = findEligibleNonClaimedActionsForPenaltyCalculation(page, queryBatchSize);
            if (eligibleActions.isEmpty()) {
                break;
            }

            page++;

            List<List<ActionPenaltyCalculationResponse>> partitions = ListUtils.partition(eligibleActions.getContent(), batchSize);

            for (List<ActionPenaltyCalculationResponse> partition : partitions) {
                Callable<Boolean> callableTask = () -> {
                    for (ActionPenaltyCalculationResponse curr : partition) {
                        try {
                            if (needsPenaltyCalculation(curr)) {
                                calculatePenalty(curr);
                            } else {
                                calculatePenaltyWithClaimAmountForAction(curr);
                            }

                        } catch (Exception e) {
                            log.error("Error while calculating penalty with ID: {} for action with ID: {}", curr.getPenaltyId(), curr.getActionId(), e);
                        }
                    }
                    return true;
                };
                callables.add(callableTask);
            }
        }

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error("Error while invoking callables", e);
            throw new RuntimeException(e);
        }
    }

    private boolean needsPenaltyCalculation(ActionPenaltyCalculationResponse calculationResponse) {
        return !calculationResponse.getActionClaimAmountManuallyEntered();
    }


    /**
     * Returns list of eligible non-claimed actions suitable for penalty calculation scheduled job.
     */
    private Page<ActionPenaltyCalculationResponse> findEligibleNonClaimedActionsForPenaltyCalculation(int page, int size) {
        return actionRepository.findEligibleNonClaimedActionsForPenaltyCalculation(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
    }


    /**
     * Calculates penalty amount (if possible) for the action.
     * This method is intended to be used in a job, as it directly updates the action.
     *
     * @param calculationResponse contains action and penalty entities
     */
    // TODO: 12/1/23 revert this method to void, when the feature is tested
    private ActionPenaltyCalculationResult calculatePenalty(ActionPenaltyCalculationResponse calculationResponse) {
        if (Objects.isNull(calculationResponse.getActionId())
                || Objects.isNull(calculationResponse.getPenaltyId())
                || Objects.isNull(calculationResponse.getPenaltyApplicability())
                || Objects.isNull(calculationResponse.getActionTypeId())
                || Objects.isNull(calculationResponse.getPenaltyActionTypeId())) {
            log.error("Action Penalty Calculation Job: inefficient information in calculation response {}", calculationResponse);
            return ActionPenaltyCalculationResult.empty("%s-Action Penalty Calculation Job: inefficient information in calculation response"
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        Action action = actionRepository
                .findById(calculationResponse.getActionId())
                .orElse(null);

        if (Objects.isNull(action)) {
            log.error("Action Penalty Calculation Job: Action with id {} not found", calculationResponse.getActionId());
            return ActionPenaltyCalculationResult.empty("%s-Action with id %s not found"
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR, calculationResponse.getActionId()));
        }

        List<Long> penaltyActionTypes = StringUtils.isEmpty(calculationResponse.getPenaltyActionTypeId())
                ? List.of() :
                Stream.of(calculationResponse.getPenaltyActionTypeId().split(","))
                        .map(Long::parseLong)
                        .toList();
        if (!penaltyActionTypes.contains(calculationResponse.getActionTypeId())) {
            log.error("Action Penalty Calculation Job: Penalty action type ID {} is required to match action's type ID {}",
                    calculationResponse.getPenaltyActionTypeId(), calculationResponse.getActionTypeId());
            emptyPenaltyCalculationFields(action);
            return ActionPenaltyCalculationResult.empty("%s-Penalty action type ID %s is required to match action's type ID %s"
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR,
                            calculationResponse.getPenaltyActionTypeId(),
                            calculationResponse.getActionTypeId()));
        }

        PenaltyFormulaEvaluationArguments arguments = PenaltyFormulaEvaluationArguments.fromCalculationResponseToArguments(calculationResponse);
        ActionPenaltyCalculationResult initialCalculationResult = penaltyFormulaEvaluationService.tryPenaltyFormulaEvaluation(arguments);

        if (initialCalculationResult.isEmpty()) {
            log.debug("Action Penalty Calculation Job: Penalty amount is not calculable for penalty with id {}", calculationResponse.getPenaltyId());
            emptyPenaltyCalculationFields(action);
            return ActionPenaltyCalculationResult.empty(initialCalculationResult.infoErrorMessages());
        }

        PenaltyTransformationByApplicabilityArguments transformationArguments = PenaltyTransformationByApplicabilityArguments
                .fromEvaluationArguments(
                        arguments,
                        initialCalculationResult,
                        calculationResponse.getActionId()
                );

        ActionPenaltyCalculationResult calculationResult = processCalculatedAmountByPenaltyApplicability(transformationArguments);

        if (Objects.isNull(calculationResult) || calculationResult.isEmpty()) {
            log.debug("Action Penalty Calculation Job: Calculated amount is empty for action with id {}", action.getId());
            emptyPenaltyCalculationFields(action);
            return calculationResult;
        }

        BigDecimal amount = calculationResult.amount().stripTrailingZeros().scale() <= 0 ?
                calculationResult.amount().setScale(0, RoundingMode.HALF_UP) : calculationResult.amount();
        action.setCalculatedPenaltyAmount(amount);
        action.setCalculatedPenaltyCurrencyId(calculationResult.currencyId());
        if (Objects.isNull(action.getPenaltyClaimAmount())) {
            action.setPenaltyClaimAmount(amount);
            action.setPenaltyClaimCurrencyId(calculationResult.currencyId());
        }
        actionRepository.saveAndFlush(action);

        /*
        liabilityGenerationService.generateLiabilityIfApplicable(
                ActionPenaltyPayer.valueOf(calculationResponse.getActionPenaltyPayer()),
                calculationResponse.getActionDontAllowAutoPenaltyClaim(),
                calculationResult,
                action.getId()
        );*/

        if (isLiabilityGenerationApplicable(ActionPenaltyPayer.valueOf(calculationResponse.getActionPenaltyPayer()),
                calculationResponse.getActionDontAllowAutoPenaltyClaim(),
                calculationResult, action.getClaimAmountManuallyEntered()
        )) {
            customerLiabilityService.createLiabilityFromAction(action.getId());
        }

        if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            actionDocumentCreationService.generateActionPenaltyDocumentAndSendEmail(action.getId());
        }

        return calculationResult;
    }

    private ActionPenaltyCalculationResult calculatePenaltyWithClaimAmountForAction(ActionPenaltyCalculationResponse calculationResponse) {
        if (Objects.isNull(calculationResponse.getActionId())
                || Objects.isNull(calculationResponse.getActionPenaltyClaimedAmount())
                || Objects.isNull(calculationResponse.getActionPenaltyClaimCurrency())
        ) {
            log.error("Action Penalty Calculation Job: inefficient information in calculation response {}", calculationResponse);
            return ActionPenaltyCalculationResult.empty("%s-Action Penalty Calculation Job: inefficient information in calculation response"
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
        }

        Action action = actionRepository
                .findById(calculationResponse.getActionId())
                .orElse(null);

        if (Objects.isNull(action)) {
            log.error("Action Penalty Calculation Job: Action with id {} not found", calculationResponse.getActionId());
            return ActionPenaltyCalculationResult.empty("%s-Action with id %s not found"
                    .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR, calculationResponse.getActionId()));
        }


        if (!customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            customerLiabilityService.createLiabilityFromAction(action.getId());
        }

        if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            actionDocumentCreationService.generateActionPenaltyDocumentAndSendEmail(action.getId());
        }

        return ActionPenaltyCalculationResult.empty("%s-Penalty amount is set from claimed amount for action with id %s"
                .formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR, calculationResponse.getActionId()));
    }

    public void testScheduler() {
        int page = 0;
        int queryBatchSize = 50;
        int batchSize = 50;
        while (true) {
            Page<ActionPenaltyCalculationResponse> eligibleActions = findEligibleNonClaimedActionsForPenaltyCalculation(page, queryBatchSize);
            if (eligibleActions.isEmpty()) {
                break;
            }

            page++;

            List<List<ActionPenaltyCalculationResponse>> partitions = ListUtils.partition(eligibleActions.getContent(), batchSize);

            for (List<ActionPenaltyCalculationResponse> partition : partitions) {
                for (ActionPenaltyCalculationResponse curr : partition) {
                    try {
                        if (needsPenaltyCalculation(curr)) {
                            calculatePenalty(curr);
                        } else {
                            calculatePenaltyWithClaimAmountForAction(curr);
                        }

                    } catch (Exception e) {
                        log.error("Error while calculating penalty with ID: {} for action with ID: {}", curr.getPenaltyId(), curr.getActionId(), e);
                    }
                }
            }
        }
    }


    /**
     * Sets penalty calculation amount and penalty calculation amount currency fields to null.
     */
    private void emptyPenaltyCalculationFields(Action action) {
        action.setCalculatedPenaltyAmount(null);
        action.setCalculatedPenaltyCurrencyId(null);
        actionRepository.save(action);
    }


    // TODO: 11/29/23 This method is only for testing purposes. Should be REMOVED for production.
    public Page<ActionPenaltyCalculationResponse> getEligibleNonClaimedActionsForPenaltyCalculation(int page, int size) {
        return findEligibleNonClaimedActionsForPenaltyCalculation(page, size);
    }

    private boolean isLiabilityGenerationApplicable(ActionPenaltyPayer penaltyPayer,
                                                    Boolean dontAllowAutomaticPenaltyClaim,
                                                    ActionPenaltyCalculationResult actionPenaltyCalculationResult,
                                                    Boolean claimAmountManuallyEntered) {
        if (!penaltyPayer.equals(EPRES) && BooleanUtils.isFalse(dontAllowAutomaticPenaltyClaim)) {
            return (actionPenaltyCalculationResult.isNotEmpty() &&
                    BooleanUtils.isTrue(actionPenaltyCalculationResult.isAutomaticClaimSelectedInPenalty())) || claimAmountManuallyEntered;
        }

        return false;
    }


    // TODO: 11/29/23 This method is only for testing purposes. Should be REMOVED for production.
    @Transactional
    public String calculatePenaltyForAction(Long actionId, boolean persist, int sizeToFilter) {
        List<ActionPenaltyCalculationResponse> nonClaimedActions = findEligibleNonClaimedActionsForPenaltyCalculation(0, sizeToFilter).getContent();

        if (CollectionUtils.isEmpty(nonClaimedActions)) {
            throw new DomainEntityNotFoundException("There are no eligible non-claimed actions suitable for penalty calculation.");
        }

        ActionPenaltyCalculationResponse calculationResponse = nonClaimedActions
                .stream()
                .filter(cr -> cr.getActionId().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new DomainEntityNotFoundException("Action with ID %s not found in the list of non-claimed actions eligible for penalty calculation.".formatted(actionId)));

        ActionPenaltyCalculationResult calculationResult = calculatePenalty(calculationResponse);

        if (persist) {
            String resultMessage = ("Penalty calculated and action persisted successfully. Calculated amount: %s, calculated currency ID: %s. " +
                    "Informational error messages: %s.")
                    .formatted(calculationResult.amount(), calculationResult.currencyId(), calculationResult.infoErrorMessages());

            boolean liabilityGenerated = liabilityGenerationService.generateLiabilityIfApplicable(
                    ActionPenaltyPayer.valueOf(calculationResponse.getActionPenaltyPayer()),
                    calculationResponse.getActionDontAllowAutoPenaltyClaim(),
                    calculationResult,
                    actionId
            );

            if (liabilityGenerated) {
                resultMessage += " Liability generated for the action.";
            }

            return resultMessage;
        } else {
            String resultMessageWhenNotPersisted = ("If you had persisted the action, calculated penalty amount would have been: %s, calculated currency ID: %s. " +
                    "Informational error messages: %s.")
                    .formatted(calculationResult.amount(), calculationResult.currencyId(), calculationResult.infoErrorMessages());

            if (liabilityGenerationService.isLiabilityGenerationApplicable(
                    ActionPenaltyPayer.valueOf(calculationResponse.getActionPenaltyPayer()),
                    calculationResponse.getActionDontAllowAutoPenaltyClaim(),
                    calculationResult
            )) {
                resultMessageWhenNotPersisted += " Liability would have been generated for the action.";
            }

            throw new OperationNotAllowedException(resultMessageWhenNotPersisted);
        }
    }

}
