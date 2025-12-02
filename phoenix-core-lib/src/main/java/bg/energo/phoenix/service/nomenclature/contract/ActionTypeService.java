package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.ActionType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.ActionTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ActionTypeResponse;
import bg.energo.phoenix.repository.nomenclature.contract.ActionTypeRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.ACTION_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionTypeService implements NomenclatureBaseService {
    private final ActionTypeRepository actionTypeRepository;


    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ACTION_TYPES;
    }


    /**
     * Adds new action type at the end of the records by default.
     * If the new action type is set to be default, the current default action type will be set to false.
     *
     * @param request the request containing the action type data
     * @return the response containing the saved action type data
     */
    @Transactional
    public ActionTypeResponse add(ActionTypeRequest request) {
        log.debug("Adding new action type: {}", request);

        if (request.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot add action type with status DELETED;");
            throw new IllegalArgumentsProvidedException("Cannot add item with status DELETED;");
        }

        if (actionTypeRepository.existsByStatusInAndNameAndId(List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE), request.getName(), null)) {
            log.error("Action type with name {} already exists;", request.getName());
            throw new IllegalArgumentsProvidedException("name-Action type with name %s already exists;".formatted(request.getName()));
        }

        Long lastOrderingId = actionTypeRepository.findLastOrderingId();
        ActionType actionType = new ActionType();
        actionType.setName(request.getName());
        actionType.setStatus(request.getStatus());
        actionType.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        actionType.setIsHardCoded(false);
        assignDefaultSelectionWhenAdding(request, actionType);
        ActionType actionTypeEntity = actionTypeRepository.save(actionType);
        return new ActionTypeResponse(actionTypeEntity);
    }


    /**
     * Assigns default selection to the action type when adding.
     * If the action type is set to be inactive, the default selection will be set to false,
     * otherwise it will be set to the value from the request and current default action type will be set to false (if exists).
     *
     * @param request    the request containing the action type data
     * @param actionType the action type entity
     */
    private void assignDefaultSelectionWhenAdding(ActionTypeRequest request, ActionType actionType) {
        if (request.getStatus().equals(INACTIVE)) {
            actionType.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<ActionType> currentDefaultItemOptional = actionTypeRepository.findByDefaultSelectionTrue();
                if (currentDefaultItemOptional.isPresent()) {
                    ActionType currentDefaultItem = currentDefaultItemOptional.get();
                    currentDefaultItem.setDefaultSelection(false);
                    actionTypeRepository.save(currentDefaultItem);
                }
            }
            actionType.setDefaultSelection(request.getDefaultSelection());
        }
    }


    /**
     * Edits existing action type if it is not hard-coded and other validations pass.
     *
     * @param id      the id of the action type to be edited
     * @param request the request containing the action type data
     * @return the response containing the edited action type data
     */
    @Transactional
    public ActionTypeResponse edit(Long id, ActionTypeRequest request) {
        log.debug("Editing action type with id: {}, {}", id, request);

        if (request.getStatus().equals(DELETED)) {
            log.error("id-You can't set status DELETED to nomeclature item;");
            throw new IllegalArgumentsProvidedException("id-You can't set status DELETED to nomeclature item;");
        }

        ActionType actionType = actionTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Action type with id %s not found;".formatted(id)));

        if (actionType.getStatus().equals(DELETED)) {
            log.error("id-You can't edit action type with status DELETED;");
            throw new IllegalArgumentsProvidedException("id-You can't edit action type with status DELETED;");
        }

        if (actionType.getIsHardCoded()) {
            log.error("id-You can't edit hard-coded nomenclature item;");
            throw new OperationNotAllowedException("id-You can't edit hard-coded nomenclature item;");
        }

        if (actionTypeRepository.existsByStatusInAndNameAndId(List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE), request.getName(), id)) {
            log.error("name-Action type with name {} already exists;", request.getName());
            throw new IllegalArgumentsProvidedException("name-Action type with name %s already exists;".formatted(request.getName()));
        }

        assignDefaultSelectionWhenEditing(request, actionType);
        actionType.setName(request.getName());
        actionType.setStatus(request.getStatus());
        actionTypeRepository.save(actionType);
        return new ActionTypeResponse(actionType);
    }


    /**
     * Assigns default selection to the action type when editing.
     * If the action type is set to be inactive, the default selection will be set to false,
     * otherwise it will be set to the value from the request and current default action type will be set to false (if exists).
     *
     * @param request    the request containing the action type data
     * @param actionType the action type entity
     */
    private void assignDefaultSelectionWhenEditing(ActionTypeRequest request, ActionType actionType) {
        if (request.getStatus().equals(INACTIVE)) {
            actionType.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!actionType.getDefaultSelection()) {
                    Optional<ActionType> currentDefaultItemOptional = actionTypeRepository.findByDefaultSelectionTrue();
                    if (currentDefaultItemOptional.isPresent()) {
                        ActionType currentDefaultItem = currentDefaultItemOptional.get();
                        currentDefaultItem.setDefaultSelection(false);
                        actionTypeRepository.save(currentDefaultItem);
                    }
                }
            }
            actionType.setDefaultSelection(request.getDefaultSelection());
        }
    }


    /**
     * Retrieves detailed information about the action type.
     *
     * @param id the id of the action type to be retrieved
     * @return the response containing the action type data
     */
    public ActionTypeResponse view(Long id) {
        log.debug("Viewing action type with id: {}", id);

        ActionType actionType = actionTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Action type with id %s not found;".formatted(id)));

        return new ActionTypeResponse(actionType);
    }


    /**
     * Retrieves all action types that match the filter criteria.
     * If excludedItemId is provided, the action type with this id will be excluded from the result.
     * If includedItemIds is provided, only the action types with these ids will be included in the result.
     * If prompt is provided, searchable field is name.
     *
     * @param request the request containing the filter criteria
     * @return the response containing the filtered action types
     */
    public Page<ActionTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering action types: {}", request);

        Page<ActionType> actionTypes = actionTypeRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return actionTypes.map(ActionTypeResponse::new);
    }


    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTION_TYPES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering action types nomenclature: {}", request);

        return actionTypeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }


    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in action types to place: {}", request.getId(), request.getOrderingId());

        ActionType actionType = actionTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Action type with id %s not found;".formatted(request.getId())));

        Long start;
        Long end;
        List<ActionType> actionTypesList;

        if (actionType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = actionType.getOrderingId();

            actionTypesList = actionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    actionType.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ActionType at : actionTypesList) {
                at.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = actionType.getOrderingId();
            end = request.getOrderingId();

            actionTypesList = actionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    actionType.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ActionType at : actionTypesList) {
                at.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        actionType.setOrderingId(request.getOrderingId());
        actionTypesList.add(actionType);
        actionTypeRepository.saveAll(actionTypesList);
    }


    /**
     * Sorts all action types alphabetically not taking into account the default selection and status.
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting action types alphabetically");

        List<ActionType> actionTypes = actionTypeRepository.orderByName();
        long orderingId = 1;

        for (ActionType actionType : actionTypes) {
            actionType.setOrderingId(orderingId);
            orderingId++;
        }

        actionTypeRepository.saveAll(actionTypes);
    }


    /**
     * Deletes action type if it is not hard-coded and other validations pass.
     *
     * @param id the id of the action type to be deleted
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Deleting action type with id: {}", id);

        ActionType actionType = actionTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Action type with id %s not found;".formatted(id)));

        if (actionType.getIsHardCoded()) {
            log.error("id-You can't delete hard-coded nomenclature item;");
            throw new OperationNotAllowedException("id-You can't delete hard-coded nomenclature item;");
        }

        if (actionType.getStatus().equals(DELETED)) {
            log.error("id-Action type with id {} is already deleted;", id);
            throw new IllegalArgumentsProvidedException("id-Action type with id %s is already deleted;".formatted(id));
        }

        if (actionTypeRepository.hasActiveConnectionsToAction(id)) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        actionType.setStatus(DELETED);
        actionTypeRepository.save(actionType);
    }


    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return actionTypeRepository.existsByIdAndStatusIn(id, statuses);
    }


    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return actionTypeRepository.findByIdIn(ids);
    }

}
