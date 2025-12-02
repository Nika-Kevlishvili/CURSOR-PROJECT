package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskTypeStage;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.TaskTypeBaseRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.TaskTypeStageRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeStageResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.nomenclature.contract.TaskTypeRepository;
import bg.energo.phoenix.repository.nomenclature.contract.TaskTypeStagesRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.service.nomenclature.mapper.TaskTypeMapperService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.permissions.PermissionContextEnum.TASK;
import static bg.energo.phoenix.permissions.PermissionContextEnum.TASK_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTypeService implements NomenclatureBaseService {
    private final TaskTypeRepository taskTypeRepository;
    private final TaskTypeStagesRepository taskTypeStagesRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final TaskTypeMapperService taskTypeMapperService;
    private final CalendarRepository calendarRepository;
    private final PortalTagRepository portalTagRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.TASK_TYPES;
    }

    /**
     * Adds {@link TaskType} at the end with the highest ordering ID.
     * If the request asks to save {@link TaskType} as a default and a default {@link TaskType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link TaskTypeBaseRequest}
     * @return {@link TaskTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public TaskTypeResponse add(TaskTypeBaseRequest request) {
        log.debug("Creating new task type");

        if (request.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot add task type with deleted status");
            throw new IllegalArgumentsProvidedException("status-Cannot add item with deleted status;");
        }

        if (taskTypeRepository.countByStatusAndName(List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE), request.getName()) > 0) {
            log.error("Task type with presented name already exists");
            throw new OperationNotAllowedException("name-Task Type with presented name already exists;");
        }

        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(request.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (calendarOptional.isEmpty()) {
            log.error("Calendar with presented id not found");
            throw new DomainEntityNotFoundException("calendarId-Calendar with presented id not found;");
        }

        Long lastOrderingId = taskTypeRepository.findLastOrderingId();
        TaskType taskType = taskTypeMapperService.mapToTaskType(request, lastOrderingId == null ? 1 : ++lastOrderingId);

        if (request.getDefaultSelection()) {
            Optional<TaskType> currentDefaultSelection = taskTypeRepository.findCurrentDefaultSelection();
            if (currentDefaultSelection.isPresent()) {
                TaskType defaultTaskType = currentDefaultSelection.get();
                defaultTaskType.setIsDefault(false);

                taskTypeRepository.save(defaultTaskType);
            }
        }

        taskTypeRepository.save(taskType);

        List<TaskTypeStageResponse> taskTypeStageResponse = new ArrayList<>();
        List<TaskTypeStageRequest> taskTypeStagesRequest = request.getTaskTypeStages();
        for (int i = 0; i < taskTypeStagesRequest.size(); i++) {
            TaskTypeStageRequest taskTypeStageRequest = taskTypeStagesRequest.get(i);
            Optional<AccountManager> accountManagerOptional = Optional.empty();
            PortalTagResponse tagResponse = null;
            if (taskTypeStageRequest.getPerformerId() != null) {
                if (taskTypeStageRequest.getPerformerType().equals(PerformerType.TAG)) {
                    Optional<PortalTag> byId = portalTagRepository.findPortalTagForGroup(taskTypeStageRequest.getPerformerId(), EntityStatus.ACTIVE);
                    if (byId.isEmpty()) {
                        throw new IllegalArgumentsProvidedException("taskTypeStages[%s].performerId-Active performer with presented id not found;".formatted(i));
                    } else {
                        tagResponse = new PortalTagResponse(byId.get());
                    }
                } else {
                    accountManagerOptional = accountManagerRepository.findByIdAndStatus(taskTypeStageRequest.getPerformerId(), List.of(Status.ACTIVE));
                    if (accountManagerOptional.isEmpty()) {
                        log.error("Active performer with presented id not found");
                        throw new IllegalArgumentsProvidedException("taskTypeStages[%s].performerId-Active performer with presented id not found;".formatted(i));
                    }
                }
            }
            TaskTypeStage taskTypeDetail = taskTypeStagesRepository.save(taskTypeMapperService.mapToTaskTypeStage(taskType.getId(), taskTypeStageRequest));
            taskTypeStageResponse.add(taskTypeMapperService.mapToTaskTypeStageResponse(taskTypeDetail, accountManagerOptional, tagResponse));
        }

        Calendar calendar = calendarOptional.get();

        return taskTypeMapperService.mapToTaskTypeResponse(taskType, new CalendarShortResponse(calendar), taskTypeStageResponse);
    }

    /**
     * Edits the {@link TaskType}.
     * If the request asks to save {@link TaskType} as a default and a default {@link TaskType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link TaskType}
     * @param request {@link TaskTypeBaseRequest}
     * @return {@link TaskTypeResponse}
     * @throws DomainEntityNotFoundException if {@link TaskType} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link TaskType} is deleted.
     */
    @Transactional
    public TaskTypeResponse edit(Long id, TaskTypeBaseRequest request) {
        TaskType taskType = taskTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task Type with presented id not found;"));

        if (request.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot set item status to deleted");
            throw new IllegalArgumentsProvidedException("status-Cannot set item status to deleted;");
        }

        if (taskType.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot edit deleted item");
            throw new OperationNotAllowedException("Cannot edit deleted item;");
        }

        if (taskTypeRepository.countByStatusAndName(List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE), request.getName()) > 0 &&
                !Objects.equals(taskType.getName(), request.getName())) {
            log.error("Task Type with presented name is already exists;");
            throw new IllegalArgumentsProvidedException("Task Type with presented name is already exists;");
        }
        taskType.setName(request.getName());

        if (request.getDefaultSelection() && !taskType.getIsDefault()) {
            Optional<TaskType> currentDefaultSelection = taskTypeRepository.findCurrentDefaultSelection();
            if (currentDefaultSelection.isPresent()) {
                TaskType defaultTaskType = currentDefaultSelection.get();
                defaultTaskType.setIsDefault(false);
                taskTypeRepository.save(defaultTaskType);
            }
        }
        taskType.setIsDefault(request.getDefaultSelection());

        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(request.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (calendarOptional.isEmpty()) {
            log.error("Calendar with presented id not found");
            throw new DomainEntityNotFoundException("calendarId-Calendar with presented id not found;");
        } else {
            Calendar calendar = calendarOptional.get();
            if (calendar.getStatus().equals(NomenclatureItemStatus.INACTIVE) && !Objects.equals(request.getCalendarId(), calendar.getId())) {
                throw new OperationNotAllowedException("calendarId-You cannot assign inactive calendar to task type;");
            }
        }

        List<TaskTypeStageResponse> taskTypeStageResponse = new ArrayList<>();

        List<TaskTypeStageRequest> taskTypeStageRequests = request.getTaskTypeStages();
        List<Long> taskTypeStageIds = taskTypeStageRequests.stream().map(TaskTypeStageRequest::getId).toList();
        List<TaskTypeStage> taskTypeStages = taskTypeStagesRepository
                .findAllByTaskTypeIdAndStatusIn(taskType.getId(), List.of(EntityStatus.ACTIVE));
        taskTypeStages
                .forEach(taskTypeStage -> {
                    if (!taskTypeStageIds.contains(taskTypeStage.getId())) {
                        taskTypeStage.setStatus(EntityStatus.DELETED);
                        taskTypeStage.setStage(null);
                    }
                });
        taskTypeStagesRepository.saveAllAndFlush(taskTypeStages);

        for (int i = 0; i < taskTypeStageRequests.size(); i++) {
            TaskTypeStageRequest taskTypeStageRequest = taskTypeStageRequests.get(i);

            Long performerId = taskTypeStageRequest.getPerformerId();

            Optional<AccountManager> accountManagerOptional = Optional.empty();
            PortalTagResponse tagResponse = null;
            if (performerId != null) {
                if (taskTypeStageRequest.getPerformerType().equals(PerformerType.TAG)) {
                    Optional<PortalTag> byId = portalTagRepository.findPortalTagForGroup(taskTypeStageRequest.getPerformerId(), EntityStatus.ACTIVE);
                    if (byId.isEmpty()) {
                        throw new DomainEntityNotFoundException("taskTypeStages[%s].performerId-Active performer with presented id not found;".formatted(i));
                    } else {
                        tagResponse = new PortalTagResponse(byId.get());
                    }
                } else {
                    accountManagerOptional = accountManagerRepository.findByIdAndStatus(taskTypeStageRequest.getPerformerId(), List.of(Status.ACTIVE));
                    if (accountManagerOptional.isEmpty()) {
                        log.error("Active performer with presented id not found");
                        throw new DomainEntityNotFoundException("taskTypeStages[%s].performerId-Active performer with presented id not found;".formatted(i));
                    }
                }
            }

            Long stageId = taskTypeStageRequest.getId();
            if (stageId == null) {
                TaskTypeStage savedTaskTypeStage = taskTypeStagesRepository
                        .save(new TaskTypeStage(
                                null,
                                taskTypeStageRequest.getPerformerType().equals(PerformerType.TAG) ? null : performerId,
                                taskTypeStageRequest.getPerformerType().equals(PerformerType.TAG) ? performerId : null,
                                taskTypeStageRequest.getPerformerType(),
                                taskTypeStageRequest.getTerm(),
                                taskTypeStageRequest.getTermType(),
                                taskTypeStageRequest.getStage(),
                                id,
                                EntityStatus.ACTIVE
                        ));

                taskTypeStageResponse.add(taskTypeMapperService.mapToTaskTypeStageResponse(savedTaskTypeStage, accountManagerOptional, tagResponse));
            } else {
                Optional<TaskTypeStage> taskTypeStageOptional = taskTypeStagesRepository.findByIdAndTaskTypeId(stageId, id);
                if (taskTypeStageOptional.isEmpty()) {
                    throw new DomainEntityNotFoundException("taskTypeStages[%s].id-Task Type Stage with presented id: [%s] not found for Task with id: [%s];".formatted(i, stageId, id));
                }

                TaskTypeStage taskTypeStage = taskTypeStageOptional.get();
                taskTypeStage.setStage(taskTypeStageRequest.getStage());
                taskTypeStage.setTermType(taskTypeStageRequest.getTermType());
                if (taskTypeStageRequest.getPerformerId() != null) {
                    if (taskTypeStageRequest.getPerformerType().equals(PerformerType.TAG)) {
                        taskTypeStage.setPerformerGroupId(performerId);
                    } else {
                        taskTypeStage.setPerformerId(performerId);
                    }
                }
                taskTypeStage.setTerm(taskTypeStageRequest.getTerm());

                taskTypeStageResponse.add(taskTypeMapperService.mapToTaskTypeStageResponse(taskTypeStage, accountManagerOptional, tagResponse));
            }
        }

        taskType.setStatus(request.getStatus());

        Calendar calendar = calendarOptional.get();
        taskType.setCalendarId(calendar.getId());

        taskTypeRepository.save(taskType);

        return taskTypeMapperService.mapToTaskTypeResponse(taskType, new CalendarShortResponse(calendar), taskTypeStageResponse);
    }

    /**
     * Retrieves detailed information about {@link TaskType} by ID
     *
     * @param id ID of {@link TaskType}
     * @return {@link TaskTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link TaskType} was found with the provided ID.
     */
    public TaskTypeResponse view(Long id) {
        TaskType taskType = taskTypeRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task Type with presented id not found;"));

        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(taskType.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));
        if (calendarOptional.isEmpty()) {
            log.error("Calendar with presented id not found");
            throw new DomainEntityNotFoundException("Calendar that assigned to task type not found;");
        }
        Calendar calendar = calendarOptional.get();

        List<TaskTypeStageResponse> taskTypeStageResponse = new ArrayList<>();
        List<TaskTypeStage> taskTypeStages = taskTypeStagesRepository.findAllByTaskTypeIdAndStatusIn(taskType.getId(), List.of(EntityStatus.ACTIVE));
        taskTypeStages.forEach(x -> {
            Optional<AccountManager> accountManagerOptional = Optional.empty();
            PortalTagResponse portalTag = null;
            if (x.getPerformerId() != null || x.getPerformerGroupId() != null) {
                if (PerformerType.MANAGER.equals(x.getPerformerType())) {
                    accountManagerOptional = accountManagerRepository.findByIdAndStatus(x.getPerformerId(), List.of(Status.ACTIVE));
                } else {
                    portalTag = new PortalTagResponse(portalTagRepository.findPortalTagForGroup(x.getPerformerGroupId(), EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("Portal tag not found!;")));
                }
            }
            taskTypeStageResponse.add(taskTypeMapperService.mapToTaskTypeStageResponse(x, accountManagerOptional, portalTag));
        });

        return taskTypeMapperService.mapToTaskTypeResponse(taskType, new CalendarShortResponse(calendar), taskTypeStageResponse);
    }

    /**
     * Filters the list of task type based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link TaskType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of TaskTypeResponse objects containing the filtered list of task type.
     */
    public Page<TaskTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering task types with request: {}", request);
        Page<TaskType> taskTypes = taskTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return taskTypes.map(taskType -> {
            TaskTypeResponse mappedValue = new TaskTypeResponse();
            mappedValue.setId(taskType.getId());
            mappedValue.setName(taskType.getName());
            mappedValue.setDefaultSelection(taskType.getIsDefault());
            mappedValue.setOrderingId(taskType.getOrderingId());
            mappedValue.setStatus(taskType.getStatus());
            mappedValue.setSystemUserId(taskType.getSystemUserId());

            List<TaskTypeStage> taskTypeStages = taskTypeStagesRepository.findAllByTaskTypeIdAndStatusIn(taskType.getId(), List.of(EntityStatus.ACTIVE));
            List<TaskTypeStageResponse> taskTypeStageResponse = new ArrayList<>(taskTypeStages.stream().map(x -> {
                if (x.getPerformerId() != null) {
                    Optional<AccountManager> accountManagerOptional = Optional.empty();
                    PortalTagResponse tagResponse = null;
                    PerformerType performerType = x.getPerformerType();
                    if (PerformerType.MANAGER.equals(performerType == null ? false : performerType)) {
                        accountManagerOptional = accountManagerRepository.findById(x.getPerformerId());
                    } else if (performerType != null) {
                        tagResponse = new PortalTagResponse(portalTagRepository.findPortalTagForGroup(x.getPerformerId(), EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("Performer not found!;")));
                    }
                    return taskTypeMapperService.mapToTaskTypeStageResponse(x, accountManagerOptional, tagResponse);
                } else {
                    return taskTypeMapperService.mapToTaskTypeStageResponse(x, Optional.empty(), null);
                }
            }).toList());

            mappedValue.setTaskTypeStages(taskTypeStageResponse);

            Calendar calendar = calendarRepository.findById(taskType.getCalendarId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Assigned calendar for task type with id %s not found"));

            mappedValue.setCalendar(new CalendarShortResponse(calendar));
            return mappedValue;
        });
    }

    /**
     * Filters {@link TaskType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link TaskType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TASK_TYPES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(
                            context = TASK,
                            permissions = {
                                    TASK_CREATE,
                                    TASK_EDIT,
                                    TASK_EDIT_SUPER_USER
                            }
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering task types with request: {}", request);
        return taskTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Changes the ordering of a {@link TaskType} item in the task type list to a specified position.
     * The method retrieves the task type item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the task type item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link TaskType} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TASK_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of task type item with ID: {} in task types to place: {}", request.getId(), request.getOrderingId());

        TaskType taskType = taskTypeRepository.findById(request.getId()).orElseThrow(() -> new DomainEntityNotFoundException("id-Task type not found with id: %s".formatted(request.getId())));

        Long start;
        Long end;
        List<TaskType> taskTypes;

        if (taskType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = taskType.getOrderingId();

            taskTypes = taskTypeRepository.findInOrderingIdRange(start, end, taskType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (TaskType t : taskTypes) {
                t.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = taskType.getOrderingId();
            end = request.getOrderingId();

            taskTypes = taskTypeRepository.findInOrderingIdRange(start, end, taskType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (TaskType t : taskTypes) {
                t.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        taskType.setOrderingId(request.getOrderingId());
        taskTypes.add(taskType);
        taskTypeRepository.saveAll(taskTypes);
    }

    /**
     * Sorts all {@link TaskType} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TASK_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the task types alphabetically");
        List<TaskType> taskTypes = taskTypeRepository.orderByName();
        long orderingId = 1;

        for (TaskType t : taskTypes) {
            t.setOrderingId(orderingId);
            orderingId++;
        }
        taskTypeRepository.saveAll(taskTypes);
    }

    /**
     * Deletes {@link TaskType} if the validations are passed.
     *
     * @param id ID of the {@link TaskType}
     * @throws DomainEntityNotFoundException if {@link TaskType} is not found.
     * @throws OperationNotAllowedException  if the {@link TaskType} is already deleted.
     * @throws OperationNotAllowedException  if the {@link TaskType} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TASK_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.info("Deleting task type with id: {}", id);
        TaskType taskType = taskTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task Type with presented id not found"));

        if (taskType.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Task Type is already deleted");
            throw new OperationNotAllowedException("Task Type with presented id already deleted");
        }

        if (taskTypeRepository.hasActiveConnections(taskType.getId())) {
            log.error("Task Type with presented id has connection with other object in system, cannot be deleted;");
            throw new OperationNotAllowedException("Task Type with presented id has connection with other object in system, cannot be deleted;");
        }

        taskType.setStatus(NomenclatureItemStatus.DELETED);
        taskTypeRepository.save(taskType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return taskTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return taskTypeRepository.findByIdIn(ids);
    }
}
