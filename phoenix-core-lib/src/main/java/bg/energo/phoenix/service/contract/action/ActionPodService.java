package bg.energo.phoenix.service.contract.action;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.action.ActionPod;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.response.contract.action.ActionPodResponse;
import bg.energo.phoenix.repository.contract.action.ActionPodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.model.enums.contract.ContractType.PRODUCT_CONTRACT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionPodService {

    private final ActionPodRepository actionPodRepository;

    /**
     * Creates action pods for the given action ID. Validation on pods is already handled before this step.
     *
     * @param podIds   List of pod IDs to be created
     * @param actionId ID of the action to be created
     */
    protected void createActionPods(List<Long> podIds, Long actionId) {
        if (CollectionUtils.isEmpty(podIds)) {
            return;
        }

        List<ActionPod> tempList = new ArrayList<>();
        for (Long podId : podIds) {
            createActionPodEntity(actionId, podId, tempList);
        }

        actionPodRepository.saveAll(tempList);
    }


    private void createActionPodEntity(Long actionId, Long podId, List<ActionPod> tempList) {
        ActionPod actionPod = new ActionPod();
        actionPod.setActionId(actionId);
        actionPod.setPodId(podId);
        actionPod.setStatus(EntityStatus.ACTIVE);
        tempList.add(actionPod);
    }


    protected List<ActionPodResponse> fetchPodsShortResponses(Long actionId) {
        return actionPodRepository.fetchShortResponseByActionIdAndStatusIn(actionId, List.of(EntityStatus.ACTIVE));
    }


    /**
     * If the requested is a service contract, and the previous version contained product contract, all persisted pods (if any) should be cleared.
     * When the requested is a product contract, the following flows should be considered:
     * <ul>
     *     <li>If the previous version has a service contract, all requested pods (if any) should be created.</li>
     *     <li>If the previous version has another product contract, persisted pods should be cleared and requested pods (if any) should be created.</li>
     *     <li>If the previous version has the same product contract, pod records (if any) should be updated. </li>
     * </ul>
     *
     * @param request Request containing the data for the action to be updated
     * @param action  Action to be updated
     */
    protected void processActionPods(ActionRequest request, Action action) {
        if (request.getContractType().equals(PRODUCT_CONTRACT)) {
            if (action.getServiceContractId() != null) {
                if (CollectionUtils.isNotEmpty(request.getPods())) {
                    createActionPods(request.getPods(), action.getId());
                }
            } else {
                if (request.getContractId().equals(action.getProductContractId())) {
                    updateActionPods(request.getPods(), action);
                } else {
                    clearActionPods(action);
                    createActionPods(request.getPods(), action.getId());
                }
            }
        } else {
            if (action.getProductContractId() != null) {
                clearActionPods(action);
            }
        }
    }


    /**
     * Updates action pods, clears removed ones and creates new ones.
     *
     * @param requestedPodIds List of pod IDs to be updated
     * @param action          Action to be updated
     */
    private void updateActionPods(List<Long> requestedPodIds, Action action) {
        if (CollectionUtils.isEmpty(requestedPodIds)) {
            clearActionPods(action);
            return;
        }

        List<ActionPod> persistedActionPods = actionPodRepository.findByActionIdAndStatusIn(action.getId(), List.of(EntityStatus.ACTIVE));
        if (CollectionUtils.isEmpty(persistedActionPods)) {
            createActionPods(requestedPodIds, action.getId());
            return;
        }

        List<Long> persistedPodIds = persistedActionPods
                .stream()
                .map(ActionPod::getPodId)
                .toList();

        List<Long> tempList = new ArrayList<>();
        for (Long podId : requestedPodIds) {
            if (!persistedPodIds.contains(podId)) {
                tempList.add(podId);
            }
        }

        createActionPods(tempList, action.getId());

        for (ActionPod pod : persistedActionPods) {
            if (!requestedPodIds.contains(pod.getPodId())) {
                pod.setStatus(EntityStatus.DELETED);
                actionPodRepository.save(pod);
            }
        }
    }


    /**
     * Clears persisted action pods.
     *
     * @param action Action to be updated
     */
    private void clearActionPods(Action action) {
        List<ActionPod> persistedActionPods = actionPodRepository.findByActionIdAndStatusIn(action.getId(), List.of(EntityStatus.ACTIVE));
        if (CollectionUtils.isNotEmpty(persistedActionPods)) {
            persistedActionPods.forEach(pod -> {
                pod.setStatus(EntityStatus.DELETED);
                actionPodRepository.save(pod);
            });
        }
    }

}
