package bg.energo.phoenix.service.contract.action;

import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.action.ActionPod;
import bg.energo.phoenix.model.entity.nomenclature.contract.ActionType;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.response.contract.action.ActionContractResponse;
import bg.energo.phoenix.model.response.contract.action.ActionPenaltyResponse;
import bg.energo.phoenix.model.response.contract.action.ActionTerminationResponse;
import bg.energo.phoenix.model.response.contract.action.calculation.PodToActionMap;
import bg.energo.phoenix.repository.contract.action.ActionPodRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ActionTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.*;

import static bg.energo.phoenix.model.enums.contract.ContractType.PRODUCT_CONTRACT;
import static bg.energo.phoenix.model.enums.contract.ContractType.SERVICE_CONTRACT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionValidatorService {

    private final ActionRepository actionRepository;
    private final ActionPodRepository actionPodRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final PenaltyRepository penaltyRepository;
    private final TerminationRepository terminationRepository;
    private final PointOfDeliveryRepository podRepository;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;


    /**
     * Runs all business logic validations against the request.
     *
     * @param request                      {@link ActionRequest} containing the data for the action to be created
     * @param actionId                     ID of the action to be updated
     * @param actionTypeStatuses           List of statuses that the action type should be in
     * @param penaltyClaimCurrencyStatuses List of statuses that the penalty claim amount currency should be in
     * @param errorMessages                List of error messages to be populated if validation fails
     */
    protected void validateRequest(ActionRequest request,
                                   Long actionId,
                                   List<NomenclatureItemStatus> actionTypeStatuses,
                                   List<NomenclatureItemStatus> penaltyClaimCurrencyStatuses,
                                   List<String> errorMessages) {
        Long respectiveContractVersion = getRespectiveContractVersion(request, errorMessages);
        validateActionType(request.getActionTypeId(), actionTypeStatuses, errorMessages);
        validatePenaltyClaimAmountCurrencyId(request.getPenaltyClaimAmountCurrencyId(), penaltyClaimCurrencyStatuses, errorMessages);
        validateActionUniqueness(request, actionId, errorMessages);
        validateContract(request, errorMessages);
        validateCustomerBelongingToRespectiveContractVersion(request, respectiveContractVersion, errorMessages);
        validatePods(request, respectiveContractVersion, errorMessages);
        validatePenalty(request, errorMessages);
        validateTermination(request, errorMessages);
        validateTemplate(request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }


    /**
     * Searches for the respective contract version by the given execution date.
     *
     * @param request       Request containing the data for the action to be created
     * @param errorMessages List of error messages to be populated if validation fails
     * @return ID of the respective contract version
     */
    private Long getRespectiveContractVersion(ActionRequest request, List<String> errorMessages) {
        Long respectiveContractVersion;
        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> respectiveContractVersion = productContractDetailsRepository
                    .getContractDetailIdByExecutionDate(
                            request.getContractId(),
                            request.getExecutionDate()
                    );
            case SERVICE_CONTRACT -> respectiveContractVersion = serviceContractDetailsRepository
                    .getContractDetailIdByExecutionDate(
                            request.getContractId(),
                            request.getExecutionDate()
                    );
            default -> {
                log.error("Contract type {} is not supported;", request.getContractType());
                errorMessages.add("contractType-Contract type %s is not supported;".formatted(request.getContractType()));
                return null;
            }
        }

        if (respectiveContractVersion == null) {
            log.error("Respective contract version not found;");
            throw new IllegalArgumentException("Respective contract version not found;");
        }

        return respectiveContractVersion;
    }


    /**
     * Validates that the action is unique on the following levels:
     * customer, contract, pod, notice receiving date, execution date, type, penalty payer, penalty.
     * If even one pod is changed, it is considered to be valid.
     *
     * @param request       Request containing the data for the action to be created
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void validateActionUniqueness(ActionRequest request, Long actionId, List<String> errorMessages) {
        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> {
                validateActionUniquenessForProductContract(request, actionId, errorMessages);
            }
            case SERVICE_CONTRACT -> {
                validateActionUniquenessForServiceContract(request, actionId, errorMessages);
            }
        }
    }


    /**
     * Validates that the action is unique, considering also the pod combinations.
     */
    private void validateActionUniquenessForProductContract(ActionRequest request, Long actionId, List<String> errorMessages) {
        List<PodToActionMap> actionsWithSameParameters = actionRepository.findActionsWithSameParametersForProductContract(
                actionId,
                request.getCustomerId(),
                request.getContractId(),
                request.getActionTypeId(),
                request.getNoticeReceivingDate(),
                request.getExecutionDate(),
                request.getPenaltyId(),
                request.getWithoutPenalty(),
                request.getPenaltyPayer().name()
        );

        if (CollectionUtils.isEmpty(actionsWithSameParameters)) {
            // if the action configuration already differs on the above level, it is considered valid and no need to check pods
            return;
        }

        for (PodToActionMap actionWithSameParameters : actionsWithSameParameters) {
            Long action = actionWithSameParameters.getActionId();
            String aggregatedPods = actionWithSameParameters.getPods();

            List<Long> persistedPods;
            if (StringUtils.isEmpty(aggregatedPods)) {
                persistedPods = Collections.emptyList();
            } else {
                persistedPods = Arrays.stream(aggregatedPods.split(";"))
                        .map(Long::parseLong)
                        .toList();
            }

            if (CollectionUtils.isEmpty(persistedPods) && CollectionUtils.isEmpty(request.getPods())) {
                log.error("Action with same parameters already exists: ID %s;".formatted(action));
                errorMessages.add("Action with same parameters already exists: ID %s;".formatted(action));
                continue;
            }

            if (CollectionUtils.isNotEmpty(persistedPods) && CollectionUtils.isNotEmpty(request.getPods())) {
                if (persistedPods.size() != request.getPods().size()) {
                    // if sizes are different, at least one pod is different, so it is considered valid
                    continue;
                }

                if (CollectionUtils.containsAll(persistedPods, request.getPods())) {
                    log.error("Action with same parameters already exists: ID %s;".formatted(action));
                    errorMessages.add("Action with same parameters already exists: ID %s;".formatted(action));
                }
            }
        }
    }


    /**
     * Validates that the action is unique, not considering also the pod combinations, because they are not applicable for service contracts.
     */
    private void validateActionUniquenessForServiceContract(ActionRequest request, Long actionId, List<String> errorMessages) {
        Long actionWithSameParams = actionRepository.findActionWithSameParametersForServiceContract(
                actionId,
                request.getCustomerId(),
                request.getContractId(), request.getActionTypeId(),
                request.getNoticeReceivingDate(),
                request.getExecutionDate(),
                request.getPenaltyId(),
                request.getWithoutPenalty(),
                request.getPenaltyPayer()
        );

        if (Objects.nonNull(actionWithSameParams)) {
            log.error("Action with same parameters already exists: ID %s;".formatted(actionWithSameParams));
            errorMessages.add("Action with same parameters already exists: ID %s;".formatted(actionWithSameParams));
        }
    }


    /**
     * Validates that any version of the customer belongs to the respective version of the contract.
     *
     * @param request                     Request containing the data for the action to be created
     * @param respectiveContractVersionId ID of the respective contract version
     * @param errorMessages               List of error messages to be populated if validation fails
     */
    private void validateCustomerBelongingToRespectiveContractVersion(ActionRequest request, Long respectiveContractVersionId, List<String> errorMessages) {
        boolean customerMatchesContract = false;
        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> customerMatchesContract = productContractDetailsRepository
                    .isCustomerAttachedToContractDetail(
                            request.getCustomerId(),
                            respectiveContractVersionId
                    );
            case SERVICE_CONTRACT -> customerMatchesContract = serviceContractDetailsRepository
                    .isCustomerAttachedToContractDetail(
                            request.getCustomerId(),
                            respectiveContractVersionId
                    );
        }

        if (!customerMatchesContract) {
            log.error("Customer does not match with the respective contract version;");
            errorMessages.add("Customer does not match with the respective contract version;");
        }
    }

    /**
     * Validates the presence of a document template when a penalty claim amount is provided.
     * If the template ID is missing while a claim amount is entered, an error is logged and
     * an error message is added to the provided error messages list.
     *
     * @param request       The ActionRequest object containing the penalty claim amount and template ID details.
     * @param errorMessages The list of error messages to which validation errors will be added.
     */
    protected void validateTemplate(ActionRequest request, List<String> errorMessages) {
        if (request.getPenaltyClaimAmount() != null && request.getTemplateId() == null) {
            log.error("No document template was selected for action while claim amount is entered;");
            errorMessages.add("No document template was selected for action while claim amount is entered;");
        }
    }

    /**
     * Validates that the penalty is available for the given action.
     *
     * @param request       Request containing the data for the action to be created
     * @param errorMessages List of error messages to be populated if validation fails
     */
    protected void validatePenalty(ActionRequest request, List<String> errorMessages) {
        if (request.getPenaltyId() == null) {
            return;
        }
        List<ActionPenaltyResponse> availablePenalties;
        if (request.getContractType().equals(PRODUCT_CONTRACT)) {
            availablePenalties = penaltyRepository.getAvailableProductPenaltiesForAction(
                            request.getContractId(),
                            request.getExecutionDate(),
                            request.getPenaltyPayer().name(),
                            EPBStringUtils.fromPromptToQueryParameter(null), // wrapping is necessary for the query to work
                            request.getPenaltyId(),
                            request.getActionTypeId(),
                            Pageable.unpaged()
                    )
                    .stream()
                    .toList();
        } else {
            availablePenalties = penaltyRepository.getAvailableServicePenaltiesForAction(
                            request.getContractId(),
                            request.getExecutionDate(),
                            request.getPenaltyPayer().name(),
                            EPBStringUtils.fromPromptToQueryParameter(null), // wrapping is necessary for the query to work
                            request.getPenaltyId(),
                            request.getActionTypeId(),
                            Pageable.unpaged()
                    )
                    .stream()
                    .toList();
        }

        if (CollectionUtils.isEmpty(availablePenalties)) {
            log.error("No available penalties were found for action;");
            errorMessages.add("No available penalties were found for action;");
            return;
        }

        Optional<ActionPenaltyResponse> penaltyOptional = availablePenalties
                .stream()
                .filter(penalty -> penalty.getId().equals(request.getPenaltyId()))
                .findFirst();

        if (penaltyOptional.isEmpty()) {
            log.error("Penalty with ID {} not available for action;", request.getPenaltyId());
            errorMessages.add("Penalty with ID %s not available for action;".formatted(request.getPenaltyId()));
        }
    }

    /**
     * Validates that the penalty is still available for the given action.
     *
     * @param action Action for which penalty must be claimed
     */
    protected void validatePenalty(Action action) {
        if (action.getPenaltyId() == null && action.getPenaltyClaimAmount() == null) {
            throw new OperationNotAllowedException("No penalty is chosen for this action;");
        }
        if (action.getPenaltyClaimAmount() == null && !penaltyRepository.isPenaltyValidForAction(action.getServiceContractId(), action.getProductContractId(),
                action.getExecutionDate(), action.getPenaltyPayer().name(),
                action.getPenaltyId(), action.getActionTypeId())) {
            log.error("Penalty with ID {} not available for action;", action.getPenaltyId());
            throw new OperationNotAllowedException("Penalty with ID %s not available for action;".formatted(action.getPenaltyId()));
        }
    }


    /**
     * Validates that the termination is available for the given action.
     *
     * @param request       Request containing the data for the action to be created
     * @param errorMessages List of error messages to be populated if validation fails
     */
    protected void validateTermination(ActionRequest request, List<String> errorMessages) {
        if (request.getTerminationId() == null) {
            return;
        }

        List<ActionTerminationResponse> availableTerminations;
        if (request.getContractType().equals(PRODUCT_CONTRACT)) {
            availableTerminations = terminationRepository.getAvailableProductTerminationsForAction(
                            request.getContractId(),
                            request.getExecutionDate(),
                            EPBStringUtils.fromPromptToQueryParameter(null), // wrapping is necessary for the query to work
                            request.getTerminationId(),
                            Pageable.unpaged()
                    )
                    .stream()
                    .toList();
        } else {
            availableTerminations = terminationRepository.getAvailableServiceTerminationsForAction(
                            request.getContractId(),
                            request.getExecutionDate(),
                            EPBStringUtils.fromPromptToQueryParameter(null), // wrapping is necessary for the query to work
                            request.getTerminationId(),
                            Pageable.unpaged()
                    )
                    .stream()
                    .toList();
        }

        if (CollectionUtils.isEmpty(availableTerminations)) {
            log.error("No available terminations were found for action;");
            errorMessages.add("No available terminations were found for action;");
            return;
        }

        Optional<ActionTerminationResponse> terminationOptional = availableTerminations
                .stream()
                .filter(termination -> termination.getId().equals(request.getTerminationId()))
                .findFirst();

        if (terminationOptional.isEmpty()) {
            log.error("Termination with ID {} not available for action;", request.getTerminationId());
            errorMessages.add("Termination with ID %s not available for action;".formatted(request.getTerminationId()));
        }
    }


    /**
     * Validates that the contract is available for the given customer.
     *
     * @param request       {@link ActionRequest} containing the data for the action to be created
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void validateContract(ActionRequest request, List<String> errorMessages) {
        List<ActionContractResponse> availableContracts = productContractRepository.filterContractsForAction(EPBStringUtils.fromPromptToQueryParameter(null), request.getCustomerId(), Pageable.unpaged()).getContent();
        if (CollectionUtils.isEmpty(availableContracts)) {
            log.error("No contracts found for action by customerId: {}", request.getCustomerId());
            errorMessages.add("contractId-No contracts found for action by customerId: %s;".formatted(request.getCustomerId()));
            return;
        }

        Optional<ActionContractResponse> contractOptional = availableContracts
                .stream()
                .filter(contract -> contract.getType().equals(request.getContractType().name()) && contract.getId().equals(request.getContractId()))
                .findFirst();

        if (contractOptional.isEmpty()) {
            log.error("Contract with ID {} not found for action by customerId: {}", request.getContractId(), request.getCustomerId());
            errorMessages.add("contractId-Contract with ID %s not found for action by customerId: %s;".formatted(request.getContractId(), request.getCustomerId()));
        }
    }


    /**
     * Validates that the pods belong to the respective contract version or any future version after it.
     *
     * @param request                     {@link ActionRequest} containing the data for the action to be created
     * @param respectiveContractVersionId ID of the respective contract version
     * @param errorMessages               List of error messages to be populated if validation fails
     */
    private void validatePods(ActionRequest request, Long respectiveContractVersionId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(request.getPods())) {
            return;
        }

        List<Long> podsBelongingToContract = podRepository.findPodsBelongingToContractForAction(
                request.getContractId(),
                respectiveContractVersionId,
                request.getPods()
        );

        List<Long> invalidPodIds = new ArrayList<>();
        for (Long podId : request.getPods()) {
            if (!podsBelongingToContract.contains(podId)) {
                invalidPodIds.add(podId);
            }
        }

        if (invalidPodIds.isEmpty()) {
            return;
        }

        List<String> invalidPodIdentifiers = podRepository.findIdentifiersByIdIn(invalidPodIds);
        log.error("Match for the respective contract version or its any future version not found for pods: {}", String.join(", ", invalidPodIdentifiers));
        errorMessages.add("Match for the respective contract version or its any future version not found for pods: %s;".formatted(String.join(", ", invalidPodIdentifiers)));
    }


    /**
     * Validates that the penalty claim amount currency is available.
     *
     * @param currencyId    ID of the currency to be validated
     * @param statuses      List of statuses that the currency should be in
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void validatePenaltyClaimAmountCurrencyId(Long currencyId, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        if (currencyId != null) {
            validateCurrency(currencyId, statuses, errorMessages);
        }
    }


    /**
     * Validates that the action type is available.
     *
     * @param actionTypeId  ID of the action type to be validated
     * @param statuses      List of statuses that the action type should be in
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void validateActionType(Long actionTypeId, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        Optional<ActionType> actionTypeOptional = actionTypeRepository.findByIdAndStatusIn(actionTypeId, statuses);
        if (actionTypeOptional.isEmpty()) {
            log.error("Action type with ID {} not found;", actionTypeId);
            errorMessages.add("actionTypeId-Action type with ID %s not found in statuses %s;".formatted(actionTypeId, statuses));
        }
    }


    /**
     * Validates that the currency is available.
     *
     * @param currencyId    ID of the currency to be validated
     * @param statuses      List of statuses that the currency should be in
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void validateCurrency(Long currencyId, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, statuses);
        if (currencyOptional.isEmpty()) {
            log.error("Currency with ID {} not found;", currencyId);
            errorMessages.add("penaltyClaimAmountCurrencyId-Currency with ID %s not found in statuses %s;".formatted(currencyId, statuses));
        }
    }


    /**
     * Validates that the user has not changed any unmodifiable fields when liability is generated and status is executed.
     *
     * @param request {@link ActionRequest} containing the data for the action to be updated
     * @param action  Action to be updated
     */
    protected void validateUnmodifiableFieldsWhenLiabilityGeneratedAndStatusExecuted(ActionRequest request, Action action) {
        boolean commonFieldsNotModified = validateCommonUnmodifiableFields(request, action);
        boolean executedFieldsNotModified = validateExecutedUnmodifiableFields(request, action);
        boolean liabilityFieldsNotModified = validateLiabilityUnmodifiableFields(request, action);

        if (!commonFieldsNotModified || !executedFieldsNotModified || !liabilityFieldsNotModified) {
            log.error("You can change only limited number of fields when action is locked because of status executed and liability is generated;");
            throw new OperationNotAllowedException("You can change only limited number of fields when action is locked because of status executed and liability is generated;");
        }
    }


    /**
     * Validates that the user has not changed any unmodifiable fields when liability is generated.
     *
     * @param request {@link ActionRequest} containing the data for the action to be updated
     * @param action  Action to be updated
     */
    protected void validateUnmodifiableFieldsWhenLiabilityGenerated(ActionRequest request, Action action) {
        boolean commonFieldsNotModified = validateCommonUnmodifiableFields(request, action);
        boolean liabilityFieldsNotModified = validateLiabilityUnmodifiableFields(request, action);

        if (!commonFieldsNotModified || !liabilityFieldsNotModified) {
            log.error("You can change only limited number of fields when action is locked because of liability generation;");
            throw new OperationNotAllowedException("You can change only limited number of fields when action is locked because of liability generation;");
        }
    }


    /**
     * Validates that the user has not changed any unmodifiable fields when status is executed.
     *
     * @param request {@link ActionRequest} containing the data for the action to be updated
     * @param action  Action to be updated
     */
    protected void validateUnmodifiableFieldsWhenStatusIsExecuted(ActionRequest request, Action action) {
        boolean commonFieldsNotModified = validateCommonUnmodifiableFields(request, action);
        boolean executedFieldsNotModified = validateExecutedUnmodifiableFields(request, action);

        if (!commonFieldsNotModified || !executedFieldsNotModified) {
            log.error("You can change only limited number of fields when action is locked because of status executed;");
            throw new OperationNotAllowedException("You can change only limited number of fields when action is locked because of status executed;");
        }
    }


    private boolean validateLiabilityUnmodifiableFields(ActionRequest request, Action action) {
        boolean isValid = Objects.equals(request.getPenaltyClaimAmount().setScale(2, RoundingMode.UNNECESSARY), action.getPenaltyClaimAmount().setScale(2, RoundingMode.UNNECESSARY));

        if (!Objects.equals(request.getPenaltyClaimAmountCurrencyId(), action.getPenaltyClaimCurrencyId())) {
            isValid = false;
        }

        if (!Objects.equals(request.getDontAllowAutomaticPenaltyClaim(), action.getDontAllowAutomaticPenaltyClaim())) {
            isValid = false;
        }

        if (!Objects.equals(request.getPenaltyId(), action.getPenaltyId())) {
            isValid = false;
        }

        if (!Objects.equals(request.getWithoutPenalty(), action.getWithoutPenalty())) {
            isValid = false;
        }

        if (!Objects.equals(request.getNoticeReceivingDate(), action.getNoticeReceivingDate())) {
            isValid = false;
        }

        if (!Objects.equals(request.getActionTypeId(), action.getActionTypeId())) {
            isValid = false;
        }

        if (!Objects.equals(request.getTerminationId(), action.getTerminationId())) {
            isValid = false;
        }

        boolean emailTemplateNotModified = validateTemplateUnmodifiableFields(ContractTemplateType.EMAIL, request, action);
        boolean documentTemplateNotModified = validateTemplateUnmodifiableFields(ContractTemplateType.DOCUMENT, request, action);

        return isValid && emailTemplateNotModified && documentTemplateNotModified;
    }

    private boolean validateTemplateUnmodifiableFields(ContractTemplateType type, ActionRequest request, Action action) {
        return (type == ContractTemplateType.EMAIL && Objects.equals(action.getEmailTemplateId(), request.getEmailTemplateId()))
                || (type == ContractTemplateType.DOCUMENT && Objects.equals(action.getTemplateId(), request.getTemplateId()));
    }


    private boolean validateExecutedUnmodifiableFields(ActionRequest request, Action action) {
        boolean isValid = Objects.equals(request.getNoticeReceivingDate(), action.getNoticeReceivingDate());

        if (!Objects.equals(request.getTerminationId(), action.getTerminationId())) {
            isValid = false;
        }

        if (!Objects.equals(request.getWithoutAutomaticTermination(), action.getWithoutAutomaticTermination())) {
            isValid = false;
        }

        return isValid;
    }


    private boolean validateCommonUnmodifiableFields(ActionRequest request, Action action) {
        boolean isValid = Objects.equals(request.getActionTypeId(), action.getActionTypeId());

        if (!Objects.equals(request.getExecutionDate(), action.getExecutionDate())) {
            isValid = false;
        }

        if (!Objects.equals(request.getPenaltyPayer(), action.getPenaltyPayer())) {
            isValid = false;
        }

        if (!Objects.equals(request.getCustomerId(), action.getCustomerId())) {
            isValid = false;
        }

        boolean contractNotModified = validateContractNotModified(request, action);
        boolean podsNotModified = validatePodsNotModified(request, action);

        return isValid && contractNotModified && podsNotModified;
    }


    private boolean validatePodsNotModified(ActionRequest request, Action action) {
        boolean isValid = true;

        List<ActionPod> persistedPods = actionPodRepository.findByActionIdAndStatusIn(action.getId(), List.of(EntityStatus.ACTIVE));
        if (CollectionUtils.isEmpty(persistedPods)) {
            if (CollectionUtils.isNotEmpty(request.getPods())) {
                isValid = false;
            }
        } else {
            if (CollectionUtils.isEmpty(request.getPods())) {
                isValid = false;
            } else {
                if (persistedPods.size() != request.getPods().size()) {
                    isValid = false;
                }

                if (!CollectionUtils.containsAll(request.getPods(), persistedPods.stream().map(ActionPod::getPodId).toList())) {
                    isValid = false;
                }
            }
        }

        return isValid;
    }


    private boolean validateContractNotModified(ActionRequest request, Action action) {
        boolean isValid = true;

        if (action.getProductContractId() != null) {
            if (!Objects.equals(request.getContractId(), action.getProductContractId())) {
                isValid = false;
            }

            if (!Objects.equals(request.getContractType(), PRODUCT_CONTRACT)) {
                isValid = false;
            }
        } else {
            if (!Objects.equals(request.getContractId(), action.getServiceContractId())) {
                isValid = false;
            }

            if (!Objects.equals(request.getContractType(), SERVICE_CONTRACT)) {
                isValid = false;
            }
        }

        return isValid;
    }

    protected void validateCalculatePenaltyAmountRequest(ActionRequest request,
                                                         List<NomenclatureItemStatus> actionTypeStatuses,
                                                         List<String> errorMessages) {
        Long respectiveContractVersion = getRespectiveContractVersion(request, errorMessages);
        validateActionType(request.getActionTypeId(), actionTypeStatuses, errorMessages);
        validatePods(request, respectiveContractVersion, errorMessages);
        validatePenalty(request, errorMessages);
        validateContract(request, errorMessages);
        validateCustomerBelongingToRespectiveContractVersion(request, respectiveContractVersion, errorMessages);
        validateTermination(request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

}
