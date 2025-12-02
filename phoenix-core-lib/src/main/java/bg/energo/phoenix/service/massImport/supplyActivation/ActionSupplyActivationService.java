package bg.energo.phoenix.service.massImport.supplyActivation;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForLocalDate;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.request.contract.pod.ActionFilterModel;
import bg.energo.phoenix.model.request.contract.pod.ActionPenaltyModel;
import bg.energo.phoenix.model.request.contract.pod.ProcessActionFilterModel;
import bg.energo.phoenix.process.model.entity.ProcessContractPods;
import bg.energo.phoenix.process.repository.ProcessContractPodRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.action.ActionService;
import bg.energo.phoenix.service.contract.product.ProductContractBillingLockValidationService;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActionSupplyActivationService {
    private final ContractPodRepository contractPodRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final ActionRepository actionRepository;
    private final ActionTypeProperties actionTypeProperties;
    private final ActionService actionService;
    private final ProcessContractPodRepository processContractPodRepository;
    private final ProductContractBillingLockValidationService productContractBillingLockValidationService;
    private final ProductContractDetailsRepository productContractDetailsRepository;

    @Transactional
    public void deactivateWithActionNew(String identifier,
                                        LocalDate deactivationDate,
                                        Long recordInfoId,
                                        Boolean hasEditLockedPermission
    ) {
        List<ActionFilterModel> models = contractPodRepository.findByPodIdentifier(identifier);
        if (models.isEmpty()) {
            throw new IllegalArgumentsProvidedException("podIdentifier-Point of delivery do not exist!;");
        }
        List<ActionFilterModel> activatedPods = models
                .stream()
                .filter(x -> x.getContractPods().getActivationDate() != null)
                .toList();
        if (activatedPods.isEmpty()) {
            throw new IllegalArgumentsProvidedException("podIdentifier-Active pod does not exist!;");
        }

        List<ActionFilterModel> filteredModels = new ArrayList<>(activatedPods
                .stream()
                .filter(x -> {
                    ContractPods contractPods = x.getContractPods();
                    LocalDate activationDate = contractPods.getActivationDate();
                    LocalDate oldDeactivationDate = contractPods.getDeactivationDate();
                    return !activationDate.isAfter(deactivationDate) && (oldDeactivationDate == null || deactivationDate.isBefore(oldDeactivationDate));
                }).toList());

        removeRestrictedModels(
                activatedPods,
                filteredModels,
                hasEditLockedPermission
        );
        removeDraftContractModels(filteredModels);

        if (filteredModels.isEmpty()) {
            List<ActionFilterModel> futurePods = activatedPods.stream()
                    .filter(x -> x.getContractPods().getActivationDate().isAfter(deactivationDate))
                    .toList();
            if (futurePods.isEmpty()) {
                throw new IllegalArgumentsProvidedException("podIdentifier-Respective Contract version for this pod is not found!;");
            } else {
                StringBuilder exceptionString = new StringBuilder();
                futurePods.forEach(model -> exceptionString.append("podIdentifier-Pod is activated in future Contract. [%s/Version: %s];".formatted(model.getContractNumber(), model.getVersionId())));
                throw new IllegalArgumentsProvidedException(exceptionString.toString());
            }
        } else if (filteredModels.size() > 1) {
            throw new IllegalArgumentsProvidedException("More than one contract found!;");
        }
        ActionFilterModel filteredModel = filteredModels.stream().findFirst().get();
        if (filteredModel.getContractPods().getDeactivationDate() != null) {
            Optional<LocalDate> startDate = contractPodRepository.findNextStartDate(filteredModel.getContractPods().getDeactivationDate(), filteredModel.getContractId());
            if (startDate.isPresent()) {
                LocalDate localDate = startDate.get();
                if (localDate.minusDays(1).equals(filteredModel.getContractPods().getDeactivationDate())) {
                    if (contractPodRepository.checkPodExistInNextVersion(filteredModel.getPodId(), filteredModel.getContractId(), localDate)) {
                        throw new IllegalArgumentsProvidedException("Pod should exist in next contract version!;");
                    }
                }
            }

        }
        ProcessContractPods processContractPods = new ProcessContractPods();
        processContractPods.setRecordInfoId(recordInfoId);
        processContractPods.setContractPodId(filteredModel.getContractPods().getId());
        processContractPodRepository.save(processContractPods);
    }

    private void removeRestrictedModels(List<ActionFilterModel> activatedPods,
                                        List<ActionFilterModel> filteredModels,
                                        Boolean hasEditLockedPermission) {
        List<Pair<Long, LocalDate>> contractIdStartDatePairs = activatedPods
                .stream()
                .map(x -> Pair.of(x.getContractId(), x.getContractPods().getActivationDate()))
                .distinct()
                .toList();
        for (Pair<Long, LocalDate> contractIdStartDatePair : contractIdStartDatePairs) {
            Boolean isRestricted = productContractBillingLockValidationService.isRestrictedForAutomations(
                    contractIdStartDatePair.getFirst(),
                    contractIdStartDatePair.getSecond(),
                    hasEditLockedPermission
            );
            if (isRestricted) {
                filteredModels.removeIf(x ->
                        x.getContractId().equals(contractIdStartDatePair.getFirst())
                                && x.getStartDate().equals(contractIdStartDatePair.getSecond()));
            }
        }
    }

    private void removeDraftContractModels(List<ActionFilterModel> filteredModels) {

        List<ActionFilterModel> toRemove = new ArrayList<>();

        for (ActionFilterModel model : filteredModels) {
            Optional<ProductContractDetails> contractDetailsOptional =
                    productContractDetailsRepository.findByContractIdAndVersionId(
                            model.getContractId(), model.getVersionId());

            if (contractDetailsOptional.isEmpty()) {
                continue;
            }
            ProductContractDetails contractDetails = contractDetailsOptional.get();

            if (contractDetails.getVersionStatus() != ProductContractVersionStatus.SIGNED) {
                toRemove.add(model);
            }
        }

        filteredModels.removeAll(toRemove);

    }

    @Transactional
    public Map<String, List<Long>> onComplete(Long processId, LocalDate deactivationDate) {
        List<ProcessActionFilterModel> actionFilterModels = processContractPodRepository.findActionModelsByProcessId(processId);
        Map<Triple<Long, Long, Long>, List<ProcessActionFilterModel>> grouped = actionFilterModels.stream().collect(Collectors.groupingBy(x -> Triple.of(x.getContractId(), x.getCustomerId(), x.getContractPods().getContractDetailId())));
        Map<String, List<Long>> messages = new HashMap<>();
        grouped.forEach((pair, model) -> {
            Long contractId = pair.getLeft();
            Long customerId = pair.getMiddle();
            Long contractDetailId = pair.getRight();
            List<Long> processInfoIds = model.stream().map(ProcessActionFilterModel::getProcessInfoId).toList();
            Map<Long, Long> podMap = model.stream().distinct().collect(Collectors.toMap(ActionFilterModel::getPodId, ActionFilterModel::getCustomerDetailId));
            Set<Long> podIds = podMap.keySet();
            Optional<CacheObjectForLocalDate> endDateOptional = productContractRepository.findContractTermEndDate(contractId);
            if (endDateOptional.isEmpty()) {
                messages.put("podIdentifier-Contract do not have term end date!;", processInfoIds);
                return;
            }
            CacheObjectForLocalDate contractTermEndDate = endDateOptional.get();
            LocalDate termEndDate = contractTermEndDate.getLocalDate();
            if (termEndDate != null && termEndDate.isEqual(deactivationDate)) {
                messages.put("podIdentifier-Action can not be created, Contract term end date equals provided deactivation date!;", processInfoIds);
                return;
            }
            //change with null safe operation

            List<CacheObject> terminationSearchResult = actionRepository.searchWithContractTermination(contractId, customerId, deactivationDate, List.of(actionTypeProperties.getContractTerminationWithNoticeId(), actionTypeProperties.getContractTerminationWithoutNoticeId()));
            if (!terminationSearchResult.isEmpty()) {
                messages.put("podIdentifier-Action can not be created, Action already exist for contract termination!;", processInfoIds);
                return;
            }

            List<ActionPenaltyModel> podSearchResult = actionRepository.searchWithAnyPod(contractId, customerId, podIds, deactivationDate, List.of(actionTypeProperties.getPodTerminationWithNoticeId()));
            if (podSearchResult.size() == podIds.size()) {
                messages.put("podIdentifier-Action can not be created, Action exists that covers this pod;", processInfoIds);
                return;
            }

            for (ActionPenaltyModel actionPenaltyModel : podSearchResult) {
                podIds.remove(actionPenaltyModel.getPodId());
            }


            //take penalties from respective version
            Set<Long> penaltyIds = productDetailsRepository
                    .findPenaltyIdsForProductWithContractDetailId(
                            contractDetailId,
                            deactivationDate,
                            List.of(actionTypeProperties.getPodTerminationWithoutNoticeId()));

            if (penaltyIds.isEmpty()) {
                messages.put("podIdentifier-No penalties found for this pods contract;", processInfoIds);
                return;
            }
            List<ActionPenaltyModel> actions = actionRepository
                    .fetchActionsForCustomerAndPenaltyAndPod(contractId, customerId, deactivationDate, penaltyIds, List.of(actionTypeProperties.getPodTerminationWithoutNoticeId()), podIds);

            Set<Long> penaltiesNotCovered = new HashSet<>(penaltyIds);
            Map<Long, Set<Long>> uncoveredPods = new HashMap<>();
            Map<Pair<Long, Long>, List<ActionPenaltyModel>> groupedByAction = actions.stream().collect(Collectors.groupingBy(x -> Pair.of(x.getActionId(), x.getPenaltyId())));
            groupedByAction.forEach((key, value) -> {
                Set<Long> podsCovered = value.stream().map(ActionPenaltyModel::getPodId).collect(Collectors.toSet());
                if (SetUtils.isEqualSet(podsCovered, podIds)) {
                    penaltiesNotCovered.remove(key.getSecond());
                    return;
                }
                Set<Long> uncoveredPodIds = uncoveredPods.get(key.getSecond());
                if (uncoveredPodIds == null) {
                    uncoveredPods.put(key.getSecond(), getDifference(podIds, podsCovered));
                } else {
                    removeContained(uncoveredPodIds, podsCovered);
                }
            });
            for (Long penaltyId : penaltiesNotCovered) {
                Set<Long> unCoveredPodIds = uncoveredPods.get(penaltyId);
                if (unCoveredPodIds == null) {
                    createActionForPods(deactivationDate, penaltyId, contractId, customerId, podIds);
                    continue;
                }
                if (unCoveredPodIds.isEmpty()) {
                    continue;
                }
                createActionForPods(deactivationDate, penaltyId, contractId, customerId, unCoveredPodIds);
            }

        });
        return messages;
    }

    public void createActionForPods(LocalDate executionDate, Long penaltyId, Long contractId, Long customerId, Set<Long> podIds) {
        ActionRequest actionRequestForPods = createActionRequestForPods(executionDate, penaltyId, customerId, contractId, podIds);
        actionService.create(actionRequestForPods);
    }

    private void removeContained(Set<Long> unCoveredPods, Set<Long> second) {
        unCoveredPods.removeIf(second::contains);
    }

    private Set<Long> getDifference(Set<Long> first, Set<Long> second) {
        Set<Long> result = new HashSet<>();
        for (Long l : first) {
            if (!second.contains(l)) {
                result.add(l);
            }
        }
        return result;
    }

    private ActionRequest createActionRequestForPods(LocalDate deactivationDate, Long penaltyId, Long customerId, Long contractId, Set<Long> podIds) {
        ActionRequest actionRequest = new ActionRequest();
        actionRequest.setActionTypeId(actionTypeProperties.getPodTerminationWithoutNoticeId());
        actionRequest.setNoticeReceivingDate(deactivationDate);
        actionRequest.setExecutionDate(deactivationDate);
        actionRequest.setPenaltyPayer(ActionPenaltyPayer.CUSTOMER);
        //Todo check this
        actionRequest.setDontAllowAutomaticPenaltyClaim(false);
        actionRequest.setPenaltyId(penaltyId);
        actionRequest.setWithoutPenalty(false);
        actionRequest.setCustomerId(customerId);
        actionRequest.setContractId(contractId);
        actionRequest.setContractType(ContractType.PRODUCT_CONTRACT);
        actionRequest.setPods(new ArrayList<>(podIds));
        return actionRequest;
    }
}
