package bg.energo.phoenix.service.task;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunTasks;
import bg.energo.phoenix.model.entity.contract.contract.ProductContractTask;
import bg.energo.phoenix.model.entity.contract.contract.ServiceContractTask;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderTask;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderTask;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationTask;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationTasks;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.CustomerTask;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskTypeStage;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgTasks;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTask;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentTasks;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyTask;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsTasks;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineTask;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingTask;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderTasks;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyTasks;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingTasks;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.entity.task.TaskComment;
import bg.energo.phoenix.model.entity.task.TaskStage;
import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.enums.task.*;
import bg.energo.phoenix.model.request.task.*;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.task.*;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunTasksRepository;
import bg.energo.phoenix.repository.contract.contract.ProductContractTaskRepository;
import bg.energo.phoenix.repository.contract.contract.ServiceContractTaskRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderTaskRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderTaskRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationTaskRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationTasksRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerTaskRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.nomenclature.contract.TaskTypeRepository;
import bg.energo.phoenix.repository.nomenclature.contract.TaskTypeStagesRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgTaskRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTaskRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentTasksRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyTaskRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestsTasksRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineTaskRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingTaskRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderTasksRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyTasksRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingTasksRepository;
import bg.energo.phoenix.repository.task.TaskCommentRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.task.TaskStageRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.service.NotificationModel;
import bg.energo.phoenix.service.notifications.service.NotificationService;
import bg.energo.phoenix.service.task.activity.TaskActivityService;
import bg.energo.phoenix.util.StringUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.permissions.PermissionContextEnum.TASK;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapperService taskMapperService;
    private final TaskRepository taskRepository;
    private final TaskStageRepository taskStageRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final TaskTypeStagesRepository taskTypeStagesRepository;
    private final ProductContractTaskRepository productContractTaskRepository;
    private final ServiceContractTaskRepository serviceContractTaskRepository;
    private final CustomerTaskRepository customerTaskRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final PermissionService permissionService;
    private final TaskActivityService taskActivityService;
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;
    private final TaskConnectedEntitiesService taskConnectedEntitiesService;
    private final ServiceOrderTaskRepository serviceOrderTaskRepository;
    private final GoodsOrderTaskRepository goodsOrderTaskRepository;
    private final BillingRunTasksRepository billingRunTasksRepository;
    private final ReceivableBlockingTaskRepository receivableBlockingTaskRepository;
    private final CustomerAssessmentTasksRepository customerAssessmentTasksRepository;
    private final EmailCommunicationTaskRepository emailCommunicationTaskRepository;
    private final SmsCommunicationTasksRepository smsCommunicationTasksRepository;
    private final DisconnectionPowerSupplyRequestsTasksRepository disconnectionPowerSupplyRequestsTasksRepository;
    private final ReconnectionOfThePowerSupplyTasksRepository reconnectionOfThePowerSupplyTasksRepository;
    private final PowerSupplyDcnCancellationTaskRepository powerSupplyDcnCancellationTaskRepository;
    private final DisconnectionPowerSupplyTaskRepository disconnectionPowerSupplyTaskRepository;
    private final PortalTagRepository portalTagRepository;
    private final ReschedulingTasksRepository reschedulingTasksRepository;
    private final NotificationService notificationService;
    private final ObjectionToChangeOfCbgTaskRepository objectionToChangeOfCbgTaskRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository;
    private final PowerSupplyDisconnectionReminderTasksRepository powerSupplyDisconnectionReminderTasksRepository;
    private final LatePaymentFineTaskRepository latePaymentFineTaskRepository;

    /**
     * Creates a new task with the given request.
     *
     * @param request The request object containing the details of the task to be created.
     * @return The ID of the newly created task.
     * @throws DomainEntityNotFoundException If the task type with the given ID is not found or is not active.
     */
    @Transactional
    public Long create(CreateTaskRequest request) {
        List<String> exceptionMessages = new ArrayList<>();
        List<String> permissionContext = new ArrayList<>(permissionService.getPermissionsFromContext(TASK));

        if (!hasPermissionForTaskCreation(request.getConnectionType(), permissionContext)) {
            throw new ClientException(
                    "You does not have permission to create task for %s;".formatted(request.getConnectionType().name()),
                    ErrorCode.ACCESS_DENIED
            );
        }

        TaskType taskType = taskTypeRepository
                .findByIdAndStatusIn(request.getTaskTypeId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("taskTypeId-Active Task Type with presented id: [%s] not found".formatted(request.getTaskTypeId()))
                );

        TaskPerformerRequest firstStage = validateFirstStage(request.getTaskPerformerRequests(), exceptionMessages);
        List<TaskTypeStage> taskTypeStages = validateTaskTypeTemplateWithPresentedTaskTypePerformers(taskType.getId(), request.getTaskPerformerRequests(), exceptionMessages);
        Task task = saveTaskWithNumber(request);

        assignConnectedEntitiesToTask(task, request, exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        saveNewTaskComment(task, request.getNewComment());
        List<TaskStage> taskStages = taskMapperService.mapFromRequestToTaskStagesEntity(task.getId(), taskTypeStages, request.getTaskPerformerRequests());
        taskStages.forEach(stage -> stage.setEndDate(calculateStageEndDate(task, stage, stage.getTerm())));
        sendCreateNotifications(firstStage, task.getId());
        List<TaskStage> savedStages = taskStageRepository.saveAllAndFlush(taskStages);
        taskStageRepository.saveAll(savedStages);

        return task.getId();
    }

    private boolean hasPermissionForTaskCreation(TaskConnectionType connectionType, List<String> permissionContext) {
        List<String> requiredPermissions = fetchPermissionsForTaskCreationByConnectionType(connectionType);
        return permissionContext.stream().anyMatch(requiredPermissions::contains);
    }

    private List<String> fetchPermissionsForTaskCreationByConnectionType(TaskConnectionType connectionType) {
        return switch (connectionType) {
            case CUSTOMER -> List.of(
                    TASK_CREATE.getId(),
                    CUSTOMER_CREATE_TASK_ON_PREVIEW.getId(),
                    CUSTOMER_ASSESSMENT_CREATE_TASK.getId()
            );
            case CONTRACT_ORDER -> List.of(
                    TASK_CREATE.getId(),
                    CONTRACT_CREATE_TASK_ON_PREVIEW.getId(),
                    ORDER_CREATE_TASK_ON_PREVIEW.getId()
            );
            case BILLING -> List.of(
                    TASK_CREATE.getId(),
                    BILLING_CREATE_TASK_ON_PREVIEW.getId()
            );
            case RECEIVABLES -> List.of(
                    TASK_CREATE.getId(),
                    RECEIVABLE_BLOCKING_CREATE_TASK_ON_PREVIEW.getId(),
                    RECONNECTION_OF_POWER_SUPPLY_CREATE_TASK.getId(),
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_TASK.getId(),
                    DISCONNECTION_OF_POWER_CREATE_TASK.getId(),
                    DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK.getId(),
                    RESCHEDULING_CREATE_TASK.getId(),
                    DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK_ON_PREVIEW.getId()
            );
            case COMMUNICATION -> List.of(
                    TASK_CREATE.getId(),
                    EMAIL_COMMUNICATION_CREATE_TASK.getId(),
                    MASS_EMAIL_COMMUNICATION_CREATE_TASK.getId(),
                    SMS_COMMUNICATION_CREATE_TASK.getId(),
                    MASS_SMS_COMMUNICATION_CREATE_TASK.getId()
            );
            case INTERNAL -> List.of(
                    TASK_CREATE.getId()
            );
            default -> List.of();
        };
    }

    /**
     * Retrieves a preview of a task identified by the given ID.
     *
     * @param id The ID of the task to retrieve the preview for.
     * @return The preview of the task.
     * @throws DomainEntityNotFoundException If the task or its associated entities are not found.
     */
    public TaskResponse preview(Long id) {
        Task task = taskRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE, EntityStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("Task with presented id: [%s] not found".formatted(id)));

        TaskType taskType = taskTypeRepository
                .findById(task.getTaskTypeId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Task Type assigned to Task not found"));

        List<TaskStage> taskStages = taskStageRepository.findAllByTaskId(task.getId());
        List<TaskComment> taskComments = taskCommentRepository.findAllByTaskId(task.getId());
        List<SystemActivityShortResponse> activities = taskActivityService.getActivitiesByConnectedObjectId(id);

        List<TaskConnectedEntityResponse> taskConnectedEntities = new ArrayList<>();
        switch (task.getConnectionType()) {
            case CONTRACT_ORDER -> addContractOrderConnectionTypes(taskConnectedEntities, id);
            case COMMUNICATION -> addCommunicationConnectionTypes(taskConnectedEntities, id);
            case RECEIVABLES -> addReceivableConnectionTypes(taskConnectedEntities, id);
            case INTERNAL -> addInternalConnectionTypes(taskConnectedEntities, id);
            case CUSTOMER -> addCustomerConnectionTypes(taskConnectedEntities, id);
            case BILLING -> addBillingConnectionTypes(taskConnectedEntities, id);
        }
        taskConnectedEntities.sort(Comparator.comparing(TaskConnectedEntityResponse::getCreateDate));

        List<TaskStageResponse> taskStageResponses = taskMapperService.mergeTaskStagesWithPerformers(taskStages, accountManagerRepository);
        List<TaskCommentHistory> commentHistory = taskMapperService.mapTaskCommentHistoryResponse(taskComments, accountManagerRepository);

        return taskMapperService.mapFromEntityToResponse(task, taskType, taskStageResponses, commentHistory, taskConnectedEntities, activities);
    }

    /**
     * Edit a task with the specified ID using the given EditTaskRequest.
     *
     * @param id      the ID of the task to edit
     * @param request the EditTaskRequest object containing the updated task details
     * @return the ID of the edited task
     * @throws ClientException if the user does not have access to change the current task
     */
    @Transactional
    public Long edit(Long id, EditTaskRequest request) {
        Task task = getTaskById(id);

        if (isSuperUser() || isCurrentPerformer(task)) {
            List<String> exceptionMessages = new ArrayList<>();

            validateTaskStatus(task);
            validateTaskTypeTemplateWithPresentedTaskTypePerformers(task.getTaskTypeId(), request.getTaskPerformerRequests(), exceptionMessages);
            validateCurrentTaskStageFields(taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1)), request, exceptionMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

            updatePerformers(task, request.getTaskPerformerRequests(), exceptionMessages);

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

            saveNewTaskComment(task, request.getNewComment());
        } else {
            throw new ClientException("You do not have access to change the current task", ErrorCode.ACCESS_DENIED);
        }

        return taskRepository.save(task).getId();
    }

    /**
     * Deletes a task by setting its status to {@link EntityStatus#DELETED}.
     * The task can only be deleted if the following conditions are met:
     * <ul>
     *     <li>The task exists and is not already deleted.</li>
     *     <li>The task is not connected to any activity.</li>
     *     <li>The task's status is {@link TaskStatus#TERMINATED}.</li>
     * </ul>
     *
     * @param id The ID of the task to be deleted.
     * @return The ID of the deleted task.
     * @throws DomainEntityNotFoundException If the task with the specified ID is not found.
     * @throws OperationNotAllowedException  If the task is already deleted, connected to an activity,
     *                                       or its status is not TERMINATED.
     */
    @Transactional
    public Long delete(Long id) {
        Task task = taskRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task with presented id not found;"));

        if (task.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Task with presented id is already deleted");
            throw new OperationNotAllowedException("Task with presented id is already deleted");
        }

        if (taskRepository.hasConnectionToActivity(id)) {
            log.error("Task with presented id has connection to activity");
            throw new OperationNotAllowedException("You cannot delete task because it is connected to an activity.");
        }

        if (!task.getTaskStatus().equals(TaskStatus.TERMINATED)) {
            log.error("Only terminated task can be deleted");
            throw new OperationNotAllowedException("Only terminated task can be deleted");
        }

        task.setStatus(EntityStatus.DELETED);

        return taskRepository.save(task).getId();
    }

    /**
     * Method used for getting listing response.
     *
     * @param taskListingRequest - Search filter
     * @return - {@link Page<TaskListingResponse>} search result.
     */
    public Page<TaskListingResponse> list(TaskListingRequest taskListingRequest) {
        log.debug("Task listing request: {}", taskListingRequest);
        Page<TaskListingMiddleResponse> middleResponsePage = taskRepository.filter(
                taskListingRequest.getColumnName() == null ? null : taskListingRequest.getColumnName().name(),
                EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(taskListingRequest.getPrompt())),
                taskListingRequest.getPerformerDirection().name(),
                taskListingRequest.getStartDateFrom(),
                taskListingRequest.getStartDateTo(),
                taskListingRequest.getEndDateFrom(),
                taskListingRequest.getEndDateTo(),
                taskListingRequest.getPerformer(),
                taskListingRequest.getConnectionType().stream().map(Enum::name).toList(),
                taskListingRequest.getCurrentPerformer(),
                taskListingRequest.getTaskTypes(),
                getTaskStatus(),
                taskListingRequest.getTaskStatuses(),
                taskListingRequest.getSortBy().getName(),
                PageRequest.of(taskListingRequest.getPage(), taskListingRequest.getSize(), taskListingRequest.getSortDirection(), taskListingRequest.getSortBy().getName())
        );
        return middleResponsePage.map(TaskListingResponse::new);
    }

    private void addCommunicationConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForSmsCommunication(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForMassSmsCommunication(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForEmailCommunication(taskId));
    }

    private void addContractOrderConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForProductContract(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForServiceContract(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForServiceOrder(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForGoodsOrder(taskId));
    }

    private void addCustomerConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForCustomer(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForCustomerAssessment(taskId));
    }

    private void addReceivableConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForDisconnectionOfPowerSupplyRequests(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForObjectionWithdrawalToChangeOfCbg(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForPowerSupplyDisconnectionCancel(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForReconnectionOfThePowerSupply(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForReminderForDisconnectionOfPws(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForDisconnectionOfPowerSupply(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForMassOperationOfBlocking(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForObjectionToChangeOfCbg(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForLatePaymentFine(taskId));
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForRescheduling(taskId));
    }

    private void addBillingConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        taskConnectedEntities.addAll(taskConnectedEntitiesService.fetchTasksForBillingRun(taskId));
    }

    private void addInternalConnectionTypes(List<TaskConnectedEntityResponse> taskConnectedEntities, Long taskId) {
        addCustomerConnectionTypes(taskConnectedEntities, taskId);
        addContractOrderConnectionTypes(taskConnectedEntities, taskId);
        addCommunicationConnectionTypes(taskConnectedEntities, taskId);
    }

    private void sendCreateNotifications(TaskPerformerRequest taskStage, Long taskId) {
        PerformerType performerType = taskStage.getPerformerType();
        List<Long> managerIds = switch (performerType) {
            case TAG -> portalTagRepository.findManagerIdsByTagId(taskStage.getPerformer(), EntityStatus.ACTIVE);
            case MANAGER -> List.of(taskStage.getPerformer());
        };
        notificationService.sendNotifications(managerIds.stream().map(x -> new NotificationModel(x, taskId, NotificationType.TASK_CREATION)).toList());
    }

    private void sendNotificationToPerformer(TaskStage taskStage, Long taskId, NotificationType type) {
        PerformerType performerType = taskStage.getPerformerType();
        List<Long> managerIds = switch (performerType) {
            case TAG -> portalTagRepository.findManagerIdsByTagId(taskStage.getPerformer(), EntityStatus.ACTIVE);
            case MANAGER -> List.of(taskStage.getPerformer());
        };
        notificationService.sendNotifications(managerIds.stream().map(x -> new NotificationModel(x, taskId, type)).toList());
    }

    private void sendNotificationToCreationUser(Task task, NotificationType type) {
        Optional<AccountManager> byUserName = accountManagerRepository.findByUserName(task.getSystemUserId());
        if (byUserName.isPresent()) {
            AccountManager accountManager = byUserName.get();
            notificationService.sendNotifications(List.of(new NotificationModel(accountManager.getId(), task.getId(), type)));
        }
    }

    private TaskPerformerRequest validateFirstStage(List<TaskPerformerRequest> taskPerformerRequests, List<String> exceptionMessages) {
        TaskPerformerRequest taskPerformerRequest = taskPerformerRequests.stream().sorted(Comparator.comparing(TaskPerformerRequest::getStage)).findFirst().get();
        validateStageRequest(exceptionMessages, taskPerformerRequest, 0);
        return taskPerformerRequest;
    }

    private void validateStageRequest(List<String> exceptionMessages, TaskPerformerRequest taskPerformerRequest, int stage) {
        if (taskPerformerRequest.getPerformer() == null) {
            exceptionMessages.add("taskPerformerRequests[%s].stage-Performer is not provided for current stage!;".formatted(stage));
            return;
        }
        if (taskPerformerRequest.getPerformerType().equals(PerformerType.TAG)) {
            if (!portalTagRepository.existsPortalTagForGroup(taskPerformerRequest.getPerformer(), EntityStatus.ACTIVE)) {
                exceptionMessages.add("taskPerformerRequests[%s].stage-Performer with id %s does not exist!;".formatted(stage, taskPerformerRequest.getPerformer()));
            }
        } else {
            if (!accountManagerRepository.existsByIdAndStatusIn(taskPerformerRequest.getPerformer(), List.of(Status.ACTIVE))) {
                exceptionMessages.add("taskPerformerRequests[%s].stage-Performer with id %s does not exist!;".formatted(stage, taskPerformerRequest.getPerformer()));
            }
        }
    }

    /**
     * Updates the status of a task. The task status can only be updated if the following conditions are met:
     * <ul>
     *     <li>The task is currently in an {@link EntityStatus#ACTIVE} state.</li>
     *     <li>The status cannot be changed to {@link TaskStatus#OVERDUE}.</li>
     *     <li>The user must either be a super user or the current performer of the task.</li>
     *     <li>The current task status must not be {@link TaskStatus#COMPLETED} or {@link TaskStatus#TERMINATED}.</li>
     * </ul>
     * <p>
     * If these conditions are met, the task's status will be updated, and the task's progress will be updated accordingly.
     *
     * @param id     The ID of the task whose status is to be updated.
     * @param status The new status to set for the task.
     * @throws OperationNotAllowedException  If the task status is attempted to be changed to {@link TaskStatus#OVERDUE},
     *                                       or if the current status of the task is {@link TaskStatus#COMPLETED} or {@link TaskStatus#TERMINATED}.
     * @throws DomainEntityNotFoundException If the task with the given ID and an active status is not found.
     * @throws ClientException               If the user does not have access to update the task status due to insufficient permissions.
     */
    @Transactional
    public void updateStatus(Long id, TaskStatus status) {
        if (status == TaskStatus.OVERDUE) {
            throw new OperationNotAllowedException("You cannot change task status to [%s]".formatted(status));
        }

        Task task = taskRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Task with presented id: [%s] not found".formatted(id)));

        if (isSuperUser() || isCurrentPerformer(task)) {
            TaskStatus taskStatus = task.getTaskStatus();
            if (taskStatus.equals(TaskStatus.COMPLETED) || taskStatus.equals(TaskStatus.TERMINATED)) {
                throw new OperationNotAllowedException("Task current status is [%s], you cannot change status".formatted(taskStatus));
            }

            TaskStage taskCurrentStage = taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1));

            updateTaskProgress(status, task, taskCurrentStage);
            taskRepository.save(task);
        } else {
            throw new ClientException("You have not access to change current task", ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Removes a sub-object from a task.
     *
     * @param taskId  The ID of the task from which the sub-object should be removed.
     * @param request The request object containing the sub-object type and ID.
     * @throws DomainEntityNotFoundException if the task with the given ID is not found.
     * @throws DomainEntityNotFoundException if the task is not assigned to the current sub-object.
     */
    @Transactional
    public void removeTaskSubObject(Long taskId, TaskRemoveSubObjectRequest request) {
        taskRepository
                .findByIdAndStatusIn(taskId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Task with presented id: [%s] not found".formatted(taskId)));

        TaskSubObjectType taskSubObjectType = request.getTaskSubObjectType();

        switch (taskSubObjectType) {
            case CUSTOMER -> {
                CustomerTask customerTask = customerTaskRepository
                        .findCustomerTaskByTaskIdAndCustomerId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current customer"));

                customerTask.setStatus(EntityStatus.DELETED);

                customerTaskRepository.save(customerTask);
            }
            case CUSTOMER_ASSESSMENT -> {
                CustomerAssessmentTasks customerAssessmentTask = customerAssessmentTasksRepository
                        .findCustomerTaskByTaskIdAndCustomerAssessmentId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current customer assessment"));

                customerAssessmentTask.setStatus(EntityStatus.DELETED);

                customerAssessmentTasksRepository.save(customerAssessmentTask);
            }
            case PRODUCT_CONTRACT -> {
                ProductContractTask productContractTask = productContractTaskRepository
                        .findProductContractTaskByTaskIdAndContractId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current Product Contract"));

                productContractTask.setStatus(EntityStatus.DELETED);

                productContractTaskRepository.save(productContractTask);
            }
            case SERVICE_CONTRACT -> {
                ServiceContractTask serviceContractTask = serviceContractTaskRepository
                        .findServiceContractTaskByTaskIdAndContractId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current Service Contract"));

                serviceContractTask.setStatus(EntityStatus.DELETED);

                serviceContractTaskRepository.save(serviceContractTask);
            }
            case GOODS_ORDER -> {
                GoodsOrderTask goodsOrderTask = goodsOrderTaskRepository
                        .findGoodsOrderTaskByTaskIdAndOrderId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current Goods Order"));

                goodsOrderTask.setStatus(EntityStatus.DELETED);

                goodsOrderTaskRepository.save(goodsOrderTask);
            }
            case SERVICE_ORDER -> {
                ServiceOrderTask serviceOrderTask = serviceOrderTaskRepository
                        .findServiceOrderTaskByTaskIdAndOrderId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current Service Order"));

                serviceOrderTask.setStatus(EntityStatus.DELETED);

                serviceOrderTaskRepository.save(serviceOrderTask);
            }
            case BILLING -> {
                BillingRunTasks billingRunTask = billingRunTasksRepository
                        .findBillingTaskByTaskIdAndBillingId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current billing"));

                billingRunTask.setStatus(EntityStatus.DELETED);

                billingRunTasksRepository.save(billingRunTask);
            }
            case RECEIVABLE_BLOCKING -> {
                ReceivableBlockingTask receivableBlockingTask = receivableBlockingTaskRepository
                        .findReceivableBlockingTaskByTaskIdAndReceivableBlockingId(taskId, request.getSubObjectId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current receivable blocking"));

                receivableBlockingTask.setStatus(ReceivableSubObjectStatus.DELETED);

                receivableBlockingTaskRepository.save(receivableBlockingTask);
            }
            case EMAIL_COMMUNICATION -> {
                EmailCommunicationTask emailCommunicationTask = emailCommunicationTaskRepository
                        .findByTaskIdAndEmailCommunicationIdAndStatus(taskId, request.getSubObjectId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current email communication"));

                emailCommunicationTask.setStatus(EntityStatus.DELETED);

                emailCommunicationTaskRepository.save(emailCommunicationTask);
            }
            case SMS_COMMUNICATION -> {
                SmsCommunicationTasks smsCommunicationTasks = smsCommunicationTasksRepository
                        .findByTaskIdAndSmsCommunicationIdAndStatusAndCommunicationChannelSingleSms(taskId, request.getSubObjectId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current sms communication"));
                smsCommunicationTasks.setStatus(EntityStatus.DELETED);
                smsCommunicationTasksRepository.save(smsCommunicationTasks);
            }
            case MASS_SMS_COMMUNICATION -> {
                SmsCommunicationTasks smsCommunicationTasks = smsCommunicationTasksRepository
                        .findByTaskIdAndSmsCommunicationIdAndStatusAndCommunicationChannel(taskId, request.getSubObjectId(), EntityStatus.ACTIVE, SmsCommunicationChannel.MASS_SMS)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current sms communication"));
                smsCommunicationTasks.setStatus(EntityStatus.DELETED);
                smsCommunicationTasksRepository.save(smsCommunicationTasks);
            }
            case DISCONNECTION_POWER_SUPPLY_REQUESTS -> {
                DisconnectionPowerSupplyRequestsTasks disconnectionPowerSupplyRequestsTasks = disconnectionPowerSupplyRequestsTasksRepository
                        .findByTaskIdAndPowerSupplyDisconnectionRequestIdAndStatus(taskId, request.getSubObjectId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current disconnection Power supply request"));

                disconnectionPowerSupplyRequestsTasks.setStatus(EntityStatus.DELETED);

                disconnectionPowerSupplyRequestsTasksRepository.save(disconnectionPowerSupplyRequestsTasks);
            }
            case RECONNECTION_OF_POWER_SUPPLY -> {
                ReconnectionOfThePowerSupplyTasks reconnectionOfThePowerSupplyTasks = reconnectionOfThePowerSupplyTasksRepository
                        .findByTaskIdAndReconnectionIdAndStatus(taskId, request.getSubObjectId(), ReceivableSubObjectStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current reconnection of Power supply request"));
                reconnectionOfThePowerSupplyTasks.setStatus(ReceivableSubObjectStatus.DELETED);
                reconnectionOfThePowerSupplyTasksRepository.save(reconnectionOfThePowerSupplyTasks);
            }
            case CANCELLATION_OF_REQUEST_FOR_DISCONNECTION_OF_PWS -> {
                PowerSupplyDcnCancellationTask cancellationTask = powerSupplyDcnCancellationTaskRepository
                        .findByIdAndPowerSupplyDcnCancellationIdAndStatus(taskId, request.getSubObjectId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current Cancellation of request for disconnection of power supply request"));
                cancellationTask.setStatus(EntityStatus.DELETED);
                powerSupplyDcnCancellationTaskRepository.save(cancellationTask);
            }
            case DISCONNECTION_OF_POWER_SUPPLY -> {
                DisconnectionPowerSupplyTask disconnectionTask = disconnectionPowerSupplyTaskRepository
                        .findByTaskIdAndPowerSupplyDisconnectionIdAndStatus(taskId, request.getSubObjectId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current disconnection of power supply request"));
                disconnectionTask.setStatus(EntityStatus.DELETED);
                disconnectionPowerSupplyTaskRepository.save(disconnectionTask);
            }
            case RESCHEDULING -> {
                ReschedulingTasks reschedulingTasks = reschedulingTasksRepository
                        .findByTaskIdAndReschedulingIdAndStatus(taskId, request.getSubObjectId(), ReceivableSubObjectStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current rescheduling"));
                reschedulingTasks.setStatus(ReceivableSubObjectStatus.DELETED);
                reschedulingTasksRepository.save(reschedulingTasks);
            }
            case OBJECTION_TO_CHANGE_OF_CBG -> {
                ObjectionToChangeOfCbgTasks objectionToChangeOfCbgTasks = taskConnectedEntitiesService.fetchTaskForObjectionToChangeOfCbg(
                        taskId,
                        request.getSubObjectId(),
                        EntityStatus.ACTIVE
                );
                objectionToChangeOfCbgTasks.setStatus(EntityStatus.DELETED);
                objectionToChangeOfCbgTaskRepository.save(objectionToChangeOfCbgTasks);
            }
            case OBJECTION_WITHDRAWAL_TO_CHANGE_OF_CBG -> {
                ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask withdrawalObjectionToChangeOfCbgTask = taskConnectedEntitiesService.fetchTaskForWithdrawalObjectionToChangeOfCbg(
                        taskId,
                        request.getSubObjectId(),
                        ReceivableSubObjectStatus.ACTIVE
                );
                withdrawalObjectionToChangeOfCbgTask.setStatus(ReceivableSubObjectStatus.DELETED);
                objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository.save(withdrawalObjectionToChangeOfCbgTask);
            }
            case REMINDER_FOR_DISCONNECTION_OF_PWS -> {
                PowerSupplyDisconnectionReminderTasks powerSupplyDisconnectionReminderTask = taskConnectedEntitiesService.fetchTaskForPowerSupplyDisconnectionReminder(
                        taskId,
                        request.getSubObjectId(),
                        ReceivableSubObjectStatus.ACTIVE
                );
                powerSupplyDisconnectionReminderTask.setStatus(ReceivableSubObjectStatus.DELETED);
                powerSupplyDisconnectionReminderTasksRepository.save(powerSupplyDisconnectionReminderTask);
            }
            case LATE_PAYMENT_FINE -> {
                LatePaymentFineTask latePaymentFineTask = taskConnectedEntitiesService.fetchTaskForLatePaymentFine(
                        taskId,
                        request.getSubObjectId(),
                        ReceivableSubObjectStatus.ACTIVE
                );
                latePaymentFineTask.setStatus(ReceivableSubObjectStatus.DELETED);
                latePaymentFineTaskRepository.save(latePaymentFineTask);
            }
        }
    }

    /**
     * Retrieves the available task statuses based on user permissions.
     * <p>
     * This method checks the user's permission context and adds the corresponding
     * task statuses to the result list. The available task statuses are determined
     * by the following permissions:
     * </p>
     * <ul>
     *   <li>{@link PermissionEnum#TASK_VIEW_DELETED}: If the user has this permission, the status
     *       {@link EntityStatus#DELETED} will be included in the result list.</li>
     *   <li>{@link PermissionEnum#TASK_VIEW_BASIC}: If the user has this permission, the status
     *       {@link EntityStatus#ACTIVE} will be included in the result list.</li>
     * </ul>
     *
     * @return A list of task statuses based on user permissions. The list can contain zero
     * or more elements.
     */
    private List<String> getTaskStatus() {
        List<String> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.TASK);
        if (context.contains(PermissionEnum.TASK_VIEW_DELETED.getId())) {
            statuses.add(EntityStatus.DELETED.name());
        }
        if (context.contains(PermissionEnum.TASK_VIEW_BASIC.getId())) {
            statuses.add(EntityStatus.ACTIVE.name());
        }
        return statuses;
    }

    /**
     * Updates the progress of a task based on the provided status, current task, and current task stage.
     *
     * @param status           The new status to set for the task.
     * @param task             The task for which the progress needs to be updated.
     * @param taskCurrentStage The current stage of the task.
     * @throws OperationNotAllowedException If the operation is not allowed based on the provided inputs.
     */
    private void updateTaskProgress(TaskStatus status, Task task, TaskStage taskCurrentStage) {
        LocalDate currentStageStartDate = taskCurrentStage.getStartDate();

        if (!status.equals(TaskStatus.TERMINATED)) {
            if (Objects.isNull(currentStageStartDate)) {
                throw new OperationNotAllowedException("Task current stage start date is not defined to update task progress");
            }

            if (Objects.isNull(taskCurrentStage.getPerformer()) && Objects.isNull(taskCurrentStage.getPerformerGroup())) {
                throw new OperationNotAllowedException("Task current stage performer is not defined to update task progress");
            }

            if (Objects.isNull(taskCurrentStage.getTerm())) {
                throw new OperationNotAllowedException("Task current stage term is not defined to update task progress");
            }

            if (currentStageStartDate.isAfter(LocalDate.now())) {
                throw new OperationNotAllowedException("You cannot update task progress for future task stages, you will be able to update status on: [%s]".formatted(currentStageStartDate));
            }
        }

        switch (taskCurrentStage.getTaskStageStatus()) {
            case OPEN -> {
                switch (status) {
                    case OPEN, COMPLETED ->
                            throw new OperationNotAllowedException("Task current status is [OPEN], you can only change to status [IN_PROGRESS,TERMINATED]");
                    case IN_PROGRESS -> inProgressTask(task, taskCurrentStage);
                    case TERMINATED -> {
                        taskCurrentStage.setCurrentPerformerId(null);
                        terminateTask(task, taskCurrentStage);
                    }
                }
            }
            case IN_PROGRESS -> {
                switch (status) {
                    case OPEN -> {
                        if (taskCurrentStage.getStage().equals(1)) {
                            task.setTaskStatus(TaskStatus.OPEN);
                        }
                        taskCurrentStage.setTaskStageStatus(TaskStageStatus.OPEN);
                        if (!task.getTaskStatus().equals(TaskStatus.OPEN)) {
                            task.setTaskStatus(TaskStatus.OPEN);
                        }
                        taskCurrentStage.setCurrentPerformerId(null);
                        taskStageRepository.save(taskCurrentStage);
                    }
                    case IN_PROGRESS ->
                            throw new OperationNotAllowedException("Task current status is [IN_PROGRESS], you can only change to status [OPEN,COMPLETED,TERMINATED]");
                    case COMPLETED -> completeTaskStage(task, taskCurrentStage);
                    case TERMINATED -> {
                        taskCurrentStage.setCurrentPerformerId(null);
                        terminateTask(task, taskCurrentStage);
                    }
                }
            }
            case OVERDUE -> {
                switch (status) {
                    case OPEN ->
                            throw new OperationNotAllowedException("Task current status is [OVERDUE], you can only change to status [COMPLETED]");
                    case IN_PROGRESS -> inProgressTask(task, taskCurrentStage);
                    case COMPLETED -> completeTaskStage(task, taskCurrentStage);
                    case TERMINATED -> {
                        taskCurrentStage.setCurrentPerformerId(null);
                        terminateTask(task, taskCurrentStage);
                    }
                }
            }
        }
    }

    private void inProgressTask(Task task, TaskStage taskCurrentStage) {
        task.setTaskStatus(TaskStatus.IN_PROGRESS);
        taskCurrentStage.setTaskStageStatus(TaskStageStatus.IN_PROGRESS);
        String loggedInUserId = permissionService.getLoggedInUserId();
        AccountManager loggedInUser = accountManagerRepository
                .findByUserName(loggedInUserId)
                .orElseThrow(() -> new ClientException("Logged in user: [%s] not found in local database".formatted(loggedInUserId), ErrorCode.APPLICATION_ERROR));
        taskCurrentStage.setCurrentPerformerId(loggedInUser.getId());
        taskStageRepository.save(taskCurrentStage);
    }

    private void terminateTask(Task task, TaskStage taskStage) {
        task.setTaskStatus(TaskStatus.TERMINATED);
        sendNotificationToCreationUser(task, NotificationType.TASK_TERMINATED);
        sendNotificationToPerformer(taskStage, task.getId(), NotificationType.TASK_TERMINATED);
    }

    /**
     * Completes the task stage and updates the task's status accordingly.
     *
     * @param task             the task to complete the stage for
     * @param taskCurrentStage the current stage of the task to be completed
     */
    private void completeTaskStage(Task task, TaskStage taskCurrentStage) {
        List<TaskStage> taskStages = taskStageRepository.findAllByTaskId(task.getId());
        taskCurrentStage.setCompletionDate(LocalDate.now());
        taskCurrentStage.setTaskStageStatus(TaskStageStatus.COMPLETED);
        taskCurrentStage.setCurrentPerformerId(null);
        taskStageRepository.save(taskCurrentStage);

        Integer currentTaskStage = taskCurrentStage.getStage();
        Optional<TaskStage> nextStageOptional = taskStages.stream().filter(taskStage -> taskStage.getStage().equals(currentTaskStage + 1)).findFirst();
        if (nextStageOptional.isPresent()) {
            TaskStage nextStage = nextStageOptional.get();
            nextStage.setStartDate(LocalDate.now());
            nextStage.setEndDate(calculateStageEndDate(task, nextStage, nextStage.getTerm()));
            sendNotificationToPerformer(nextStage, task.getId(), NotificationType.TASK_STAGE_COMPLETION_NEXT_PERFORMERS);
            taskStageRepository.save(nextStage);
            task.setTaskStatus(TaskStatus.OPEN);
        } else {
            task.setTaskStatus(TaskStatus.COMPLETED);
            sendNotificationToCreationUser(task, NotificationType.TASK_COMPLETION);
        }
    }

    /**
     * Validates the task type template with the presented task type performers.
     *
     * @param taskTypeId            The ID of the task type.
     * @param taskPerformerRequests The list of task performer requests.
     * @param exceptionMessages     The list to collect exception messages.
     * @return The list of task type stages.
     */
    private List<TaskTypeStage> validateTaskTypeTemplateWithPresentedTaskTypePerformers(Long taskTypeId, List<TaskPerformerRequest> taskPerformerRequests, List<String> exceptionMessages) {
        List<TaskTypeStage> taskTypeStages = taskTypeStagesRepository
                .findAllByTaskTypeIdAndStatusIn(taskTypeId, List.of(EntityStatus.ACTIVE));

        int stagesSize = taskTypeStages
                .size();

        if (taskPerformerRequests.size() != stagesSize) {
            exceptionMessages.add("taskPerformerRequests-Presented task type performers doesn't match with presented Task Type Nomenclature;");
        }

        List<Integer> taskTypeStageOrders = taskTypeStages.stream().map(TaskTypeStage::getStage).toList();
        for (int i = 0; i < taskPerformerRequests.size(); i++) {
            TaskPerformerRequest taskPerformerRequest = taskPerformerRequests.get(i);

            if (!taskTypeStageOrders.contains(taskPerformerRequest.getStage())) {
                exceptionMessages.add("taskPerformerRequests[%s].stage-Presented task type performers doesn't match with presented Task Type Nomenclature;".formatted(i));
            } else {
                List<TaskTypeStage> taskTypeStageList = taskTypeStages
                        .stream()
                        .filter(taskTypeStage -> taskPerformerRequest.getStage().equals(taskTypeStage.getStage()))
                        .toList();

                if (taskTypeStageList.size() > 1) {
                    exceptionMessages.add("taskPerformerRequests[%s].stage-Duplicated stage found in Task Type Nomenclature;".formatted(i));
                    continue;
                }
                TaskTypeStage taskTypeStage = taskTypeStageList.get(0);
                if (taskPerformerRequest.getPerformerType() != null && taskTypeStage.getPerformerType() != null) {
                    if (!taskPerformerRequest.getPerformerType().equals(taskTypeStage.getPerformerType())) {
                        exceptionMessages.add("taskPerformerRequests[%s].stage-Presented task type performers doesn't match with presented Task Type Nomenclature;".formatted(i));
                    } else if (taskPerformerRequest.getPerformerType().equals(PerformerType.TAG)) {
                        if (!taskTypeStage.getPerformerGroupId().equals(taskPerformerRequest.getPerformer())) {
                            exceptionMessages.add("taskPerformerRequests[%s].stage-Presented task type performers doesn't match with presented Task Type Nomenclature;".formatted(i));
                        }
                    } else if (taskPerformerRequest.getPerformerType().equals(PerformerType.MANAGER)) {
                        if (!taskTypeStage.getPerformerId().equals(taskPerformerRequest.getPerformer())) {
                            exceptionMessages.add("taskPerformerRequests[%s].stage-Presented task type performers doesn't match with presented Task Type Nomenclature;".formatted(i));
                        }
                    }
                }
                if (taskPerformerRequest.getPerformerType() != null) {
                    switch (taskPerformerRequest.getPerformerType()) {
                        case TAG -> {
                            if (!portalTagRepository.existsPortalTagForGroup(taskPerformerRequest.getPerformer(), EntityStatus.ACTIVE)) {
                                exceptionMessages.add("taskPerformerRequests[%s].performer-Presented task type performer doesn't exist or is not group tag;".formatted(i));
                            }
                        }
                        case MANAGER -> {
                            if (!accountManagerRepository.existsByIdAndStatusIn(taskPerformerRequest.getPerformer(), List.of(Status.ACTIVE))) {
                                exceptionMessages.add("taskPerformerRequests[%s].performer-Presented task type performer doesn't exist ;".formatted(i));
                            }
                        }
                    }
                }

            }
        }

        return taskTypeStages;
    }

    /**
     * Saves a task with a given number.
     *
     * @param request the create task request containing the details of the task
     * @return the saved task
     */
    private Task saveTaskWithNumber(CreateTaskRequest request) {
        Long nextSequenceValue = taskRepository.getNextSequenceValue();
        return taskRepository.save(taskMapperService.mapFromRequestToTaskEntity(nextSequenceValue, request));
    }

    /**
     * Updates the performers of the given task stages based on the provided performer requests.
     *
     * @param task                  The task for which the performers need to be updated.
     * @param taskPerformerRequests The list of performer requests containing the stages and corresponding performers.
     * @param exceptionMessages     The list to store any exception messages encountered during the update process.
     */
    private void updatePerformers(Task task, List<TaskPerformerRequest> taskPerformerRequests, List<String> exceptionMessages) {
        boolean isSuperUser = isSuperUser();
        Range<Long> termRange = Range.between(0L, 99L);

        if (isSuperUser) {
            List<TaskStage> taskStages = taskStageRepository
                    .findAllByTaskId(task.getId());

            for (TaskStage taskStage : taskStages) {
                List<TaskPerformerRequest> existingTaskStages = taskPerformerRequests
                        .stream()
                        .filter(taskPerformerRequest -> taskPerformerRequest.getStage().equals(taskStage.getStage()))
                        .toList();
                if (existingTaskStages.isEmpty()) {
                    throw new IllegalArgumentsProvidedException("Invalid stages presented"); // This exception must never be thrown because, validation prevent it before
                } else if (existingTaskStages.size() > 1) {
                    throw new IllegalArgumentsProvidedException("Duplicated stages"); // This exception must never be thrown because, validation prevent it before
                } else {
                    TaskPerformerRequest taskPerformerRequest = existingTaskStages.get(0);
                    boolean validationSuccess = true;
                    if (taskPerformerRequest.getPerformer() != null) {
                        if (PerformerType.MANAGER.equals(taskPerformerRequest.getPerformerType()) && accountManagerRepository.findByIdAndStatus(taskPerformerRequest.getPerformer(), List.of(Status.ACTIVE)).isEmpty()) {
                            exceptionMessages.add("taskPerformerRequests[%s].performer-Performer with presented id not found".formatted(taskPerformerRequests.indexOf(taskPerformerRequest)));
                            validationSuccess = false;
                        } else if (PerformerType.TAG.equals(taskPerformerRequest.getPerformerType()) && !portalTagRepository.existsPortalTagForGroup(taskPerformerRequest.getPerformer(), EntityStatus.ACTIVE)) {
                            exceptionMessages.add("taskPerformerRequests[%s].performer-Performer with presented id not found".formatted(taskPerformerRequests.indexOf(taskPerformerRequest)));
                            validationSuccess = false;
                        }
                    }
                    if (validationSuccess) {
                        if (taskStage.getTaskStageStatus().equals(TaskStageStatus.COMPLETED)) {
                            boolean performerChanged = !Objects.equals(taskStage.getPerformer(), taskPerformerRequest.getPerformer());
                            boolean performerTypeChanged = !Objects.equals(taskStage.getPerformerType(), taskPerformerRequest.getPerformerType());
                            boolean startDateChanged = !Objects.equals(taskStage.getStartDate(), taskPerformerRequest.getStartDate());
                            boolean termValueChanged = !Objects.equals(taskStage.getTerm(), taskPerformerRequest.getTerm());

                            if (performerChanged || performerTypeChanged) {
                                exceptionMessages.add("taskPerformerRequests[%s].performer-You can change COMPLETED task stage details;".formatted(taskPerformerRequests.indexOf(taskPerformerRequest)));
                            }

                            if (startDateChanged) {
                                exceptionMessages.add("taskPerformerRequests[%s].startDate-You can change COMPLETED task stage details;".formatted(taskPerformerRequests.indexOf(taskPerformerRequest)));
                            }

                            if (termValueChanged) {
                                exceptionMessages.add("taskPerformerRequests[%s].term-You can change COMPLETED task stage details;".formatted(taskPerformerRequests.indexOf(taskPerformerRequest)));
                            }
                        } else {
                            Long term = taskPerformerRequest.getTerm();

                            taskStage.setPerformer(PerformerType.TAG.equals(taskPerformerRequest.getPerformerType()) ? null : taskPerformerRequest.getPerformer());
                            taskStage.setPerformerGroup(PerformerType.TAG.equals(taskPerformerRequest.getPerformerType()) ? taskPerformerRequest.getPerformer() : null);
                            taskStage.setPerformerType(taskPerformerRequest.getPerformerType());
                            taskStage.setStartDate(taskPerformerRequest.getStartDate());

                            taskStage.setTerm(term);
                            taskStage.setEndDate(calculateStageEndDate(task, taskStage, term));
                        }
                    }
                }
            }
            taskStageRepository.saveAll(taskStages);
        } else {
            TaskStage taskCurrentStage = taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1));
            List<TaskStage> taskStages = taskStageRepository.findAllByTaskId(task.getId());

            List<TaskPerformerRequest> currentUserTaskPerformerRequest = taskPerformerRequests
                    .stream()
                    .filter(taskPerformerRequest -> taskPerformerRequest.getStage().equals(taskCurrentStage.getStage()))
                    .toList();

            if (currentUserTaskPerformerRequest.isEmpty()) {
                throw new IllegalArgumentsProvidedException("Invalid stages presented"); // This exception must never be thrown because, validation prevent it before
            } else if (currentUserTaskPerformerRequest.size() > 1) {
                throw new IllegalArgumentsProvidedException("Duplicated stages"); // This exception must never be thrown because, validation prevent it before
            } else {
                TaskPerformerRequest currentTaskPerformerRequest = currentUserTaskPerformerRequest.get(0);

                if (isCurrentPerformer(task)) {
                    Long performer = currentTaskPerformerRequest.getPerformer();

                    if (PerformerType.MANAGER.equals(currentTaskPerformerRequest.getPerformerType()) && accountManagerRepository.findByIdAndStatus(performer, List.of(Status.ACTIVE)).isPresent()) {
                        taskCurrentStage.setPerformer(performer);
                        taskCurrentStage.setPerformerType(currentTaskPerformerRequest.getPerformerType());
                        taskCurrentStage.setTerm(currentTaskPerformerRequest.getTerm());
                        taskCurrentStage.setStartDate(currentTaskPerformerRequest.getStartDate());
                        taskCurrentStage.setEndDate(calculateStageEndDate(task, taskCurrentStage, currentTaskPerformerRequest.getTerm()));
                    } else if (PerformerType.TAG.equals(currentTaskPerformerRequest.getPerformerType()) && portalTagRepository.existsPortalTagForGroup(performer, EntityStatus.ACTIVE)) {
                        taskCurrentStage.setPerformerGroup(performer);
                        taskCurrentStage.setPerformerType(currentTaskPerformerRequest.getPerformerType());
                        taskCurrentStage.setTerm(currentTaskPerformerRequest.getTerm());
                        taskCurrentStage.setStartDate(currentTaskPerformerRequest.getStartDate());
                        taskCurrentStage.setEndDate(calculateStageEndDate(task, taskCurrentStage, currentTaskPerformerRequest.getTerm()));
                    } else {
                        exceptionMessages.add("taskPerformerRequests[%s].performer-Performer with presented id not found;".formatted(taskPerformerRequests.indexOf(currentTaskPerformerRequest)));
                    }

                    taskStageRepository.save(taskCurrentStage);
                } else {
                    boolean performerChanged = !Objects.equals(taskCurrentStage.getPerformer(), currentTaskPerformerRequest.getPerformer());
                    boolean performerTypeChanged = !Objects.equals(taskCurrentStage.getPerformerType(), currentTaskPerformerRequest.getPerformerType());
                    boolean startDateChanged = !Objects.equals(taskCurrentStage.getStartDate(), currentTaskPerformerRequest.getStartDate());
                    boolean termValueChanged = !Objects.equals(taskCurrentStage.getTerm(), currentTaskPerformerRequest.getTerm());

                    if (performerChanged || performerTypeChanged) {
                        exceptionMessages.add("taskPerformerRequests[%s].performer-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(currentTaskPerformerRequest)));
                    }

                    if (startDateChanged) {
                        exceptionMessages.add("taskPerformerRequests[%s].startDate-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(currentTaskPerformerRequest)));
                    }

                    if (termValueChanged) {
                        exceptionMessages.add("taskPerformerRequests[%s].term-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(currentTaskPerformerRequest)));
                    }

                    if (!termRange.contains(currentTaskPerformerRequest.getTerm())) {
                        exceptionMessages.add("taskPerformerRequests[%s].term-Term must be in range: [{%s}:{%s}];".formatted(taskPerformerRequests.indexOf(currentTaskPerformerRequest), termRange.getMinimum(), termRange.getMaximum()));
                    }
                }

                taskStages.remove(taskCurrentStage);

                for (TaskStage taskStage : taskStages) {
                    List<TaskPerformerRequest> existingTaskStages = taskPerformerRequests
                            .stream()
                            .filter(taskPerformerRequest -> taskPerformerRequest.getStage().equals(taskStage.getStage()))
                            .toList();
                    if (existingTaskStages.isEmpty()) {
                        throw new IllegalArgumentsProvidedException("Invalid stages presented"); // This exception must never be thrown because, validation prevent it before
                    } else if (existingTaskStages.size() > 1) {
                        throw new IllegalArgumentsProvidedException("Duplicated stages"); // This exception must never be thrown because, validation prevent it before
                    } else {
                        if (!Objects.equals(taskStage.getPerformer(), existingTaskStages.get(0).getPerformer()) || !Objects.equals(taskStage.getPerformerType(), existingTaskStages.get(0).getPerformerType())) {
                            exceptionMessages.add("taskPerformerRequests[%s].performer-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(existingTaskStages.get(0))));
                        }
                        if (!Objects.equals(taskStage.getStartDate(), existingTaskStages.get(0).getStartDate())) {
                            exceptionMessages.add("taskPerformerRequests[%s].startDate-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(existingTaskStages.get(0))));
                        }
                        if (!Objects.equals(taskStage.getTerm(), existingTaskStages.get(0).getTerm())) {
                            exceptionMessages.add("taskPerformerRequests[%s].term-You can change only your stage details, please contact to super user;".formatted(taskPerformerRequests.indexOf(existingTaskStages.get(0))));
                        }
                    }

                    taskStage.setEndDate(calculateStageEndDate(task, taskStage, taskStage.getTerm()));
                }

                taskStageRepository.saveAll(taskStages);
            }
        }
    }

    /**
     * Validates the fields of the current task stage in the given task and request.
     * If any field validation fails, the corresponding exception messages are added to the provided list.
     *
     * @param taskCurrentStage  The current stage of the task.
     * @param request           The EditTaskRequest object containing the task performer requests.
     * @param exceptionMessages The list to store the exception messages.
     */
    private void validateCurrentTaskStageFields(TaskStage taskCurrentStage, EditTaskRequest request, List<String> exceptionMessages) {
        Integer stage = taskCurrentStage.getStage();
        Optional<TaskPerformerRequest> currentTaskPerformerRequestOptional = request
                .getTaskPerformerRequests()
                .stream()
                .filter(taskPerformerRequest -> taskPerformerRequest.getStage().equals(stage))
                .findFirst();
        TaskPerformerRequest currentTaskPerformerRequest;
        if (currentTaskPerformerRequestOptional.isPresent()) {
            currentTaskPerformerRequest = currentTaskPerformerRequestOptional
                    .get();
        } else {
            exceptionMessages.add("Current task stage cannot be found;");
            return;
        }

        int stageIndex = request.getTaskPerformerRequests().indexOf(currentTaskPerformerRequest);
        if (currentTaskPerformerRequest.getStartDate() == null) {
            exceptionMessages.add("taskPerformerRequests[%s].startDate-Start date of current performer must not be null;".formatted(stageIndex));
        }

        if (currentTaskPerformerRequest.getPerformer() == null) {
            exceptionMessages.add("taskPerformerRequests[%s].performer-Performer must not be null for current Task Stage;".formatted(stageIndex));
        }
        validateStageRequest(exceptionMessages, currentTaskPerformerRequest, stageIndex);
    }

    /**
     * Calculates the end date of a stage based on the task, stage, and term.
     *
     * @param task      The task associated with the stage.
     * @param taskStage The stage for which the end date is being calculated.
     * @param term      The term in days or working days.
     * @return The end date of the stage.
     */
    private LocalDate calculateStageEndDate(Task task, TaskStage taskStage, Long term) {
        if (term == null) {
            return null;
        }

        LocalDate endDate = null;

        if (taskStage.getStartDate() != null) {
            TermType termType = taskStage.getTermType();

            switch (termType) {
                case CALENDAR_DAYS -> {
                    if (term != 0) {
                        endDate = taskStage.getStartDate().plusDays(term - 1);
                    } else {
                        endDate = taskStage.getStartDate();
                    }
                }
                case WORKING_DAYS -> {
                    long termIterator = term;

                    Long taskTypeId = task.getTaskTypeId();
                    TaskType taskType = taskTypeRepository.findByIdAndStatusIn(taskTypeId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                            .orElseThrow(() -> new DomainEntityNotFoundException("Presented task type does not exists"));
                    Long calendarId = taskType.getCalendarId();
                    Calendar calendar = calendarRepository
                            .findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                            .orElseThrow(() -> new DomainEntityNotFoundException("Presented Task Types calendar not found;"));
                    List<DayOfWeek> weekends =
                            Arrays.stream(
                                            Objects.requireNonNullElse(calendar.getWeekends(), "")
                                                    .split(";")
                                    )
                                    .filter(StringUtils::isNotBlank)
                                    .map(DayOfWeek::valueOf)
                                    .toList();
                    List<Holiday> holidays = holidaysRepository.findAllByCalendarId(calendarId);
                    if (term != 0) {
                        endDate = taskStage.getStartDate().minusDays(1);
                    } else {
                        endDate = taskStage.getStartDate();
                    }
                    while (termIterator != 0) {
                        LocalDate nextDay = endDate.plusDays(1);
                        if (isWorkingDay(nextDay, weekends, holidays)) {
                            termIterator--;
                        }
                        endDate = nextDay;
                    }
                }
            }
        }

        return endDate;
    }

    /**
     * Checks if a given date is a working day, considering a list of weekends and holidays.
     *
     * @param date     the date to be checked
     * @param weekends the list of weekends as a list of DayOfWeek objects
     * @param holidays the list of holidays as a list of Holiday objects
     * @return true if the given date is a working day, false otherwise
     */
    private boolean isWorkingDay(LocalDate date, List<DayOfWeek> weekends, List<Holiday> holidays) {
        List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHoliday).toList().stream().map(LocalDateTime::toLocalDate).toList();
        return !holidayDates.contains(date) && !weekends.contains(date.getDayOfWeek());
    }

    /**
     * Checks if the current user has super user privileges.
     *
     * @return {@code true} if the current user is a super user, {@code false} otherwise.
     */
    private boolean isSuperUser() {
        return permissionService
                .permissionContextContainsPermissions(PermissionContextEnum.TASK, List.of(TASK_EDIT_SUPER_USER));
    }

    /**
     * Determines whether the logged-in user is the current performer of the given task.
     *
     * @param task The task for which to check the current performer.
     * @return {@code true} if the logged-in user is the current performer of the task,
     * {@code false} otherwise.
     * @throws ClientException If the logged-in user is not found in the local database.
     */
    private boolean isCurrentPerformer(Task task) {
        TaskStage taskCurrentStage = taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1));
        String loggedInUserId = permissionService.getLoggedInUserId();

        AccountManager loggedInUser = accountManagerRepository
                .findByUserName(loggedInUserId)
                .orElseThrow(() -> new ClientException("Logged in user: [%s] not found in local database".formatted(loggedInUserId), ErrorCode.APPLICATION_ERROR));

        LocalDate now = LocalDate.now();
        LocalDate startDate = taskCurrentStage.getStartDate();
        LocalDate endDate = taskCurrentStage.getEndDate();

        if (isDateInRange(now, startDate, endDate)) {
            if (taskCurrentStage.getPerformerType().equals(PerformerType.MANAGER)) {
                return taskCurrentStage.getPerformer().equals(loggedInUser.getId());
            } else {
                return accountManagerRepository.managerHasTag(taskCurrentStage.getPerformerGroup(), loggedInUser.getId());
            }
        }

        return false;
    }

    public Boolean isDateInRange(LocalDate dateToCheck, LocalDate startDate, LocalDate endDate) {
        if (Objects.isNull(endDate)) {
            return !startDate.isBefore(dateToCheck);
        } else {
            return !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate);
        }
    }

    /**
     * Assigns connected entities to a task based on the provided request.
     *
     * @param task              The task to assign the connected entities to.
     * @param request           The request containing the connected entities information.
     * @param exceptionMessages A list to store any exception messages encountered during the assignment.
     */
    private void assignConnectedEntitiesToTask(Task task, CreateTaskRequest request, List<String> exceptionMessages) {
        List<TaskConnectedEntity> connectedEntities = request.getConnectedEntities();

        switch (request.getConnectionType()) {
            case CUSTOMER -> {
                for (int i = 0; i < connectedEntities.size(); i++) {
                    TaskConnectedEntity taskConnectedEntity = connectedEntities.get(i);
                    saveCustomerConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, false);
                }
            }
            case CONTRACT_ORDER -> {
                for (int i = 0; i < connectedEntities.size(); i++) {
                    TaskConnectedEntity taskConnectedEntity = connectedEntities.get(i);
                    saveContractOrderConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, false);
                }
            }
            case BILLING ->
                    taskConnectedEntitiesService.createTaskBillingRun(task, connectedEntities, exceptionMessages);
            case COMMUNICATION -> {
                for (int i = 0; i < connectedEntities.size(); i++) {
                    TaskConnectedEntity taskConnectedEntity = connectedEntities.get(i);
                    saveCommunicationConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, false);
                }
            }
            case RECEIVABLES -> {
                for (int i = 0; i < connectedEntities.size(); i++) {
                    TaskConnectedEntity taskConnectedEntity = connectedEntities.get(i);
                    switch (taskConnectedEntity.getEntityType()) {
                        case RECEIVABLE_BLOCKING ->
                                taskConnectedEntitiesService.createTaskReceivableBlocking(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case DISCONNECTION_POWER_SUPPLY_REQUESTS ->
                                taskConnectedEntitiesService.createTaskDisconnectionPowerSupplyRequest(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case RECONNECTION_OF_POWER_SUPPLY ->
                                taskConnectedEntitiesService.createTaskReconnectionOfThePowerSupply(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case CANCELLATION_OF_REQUEST_FOR_DISCONNECTION_OF_PWS ->
                                taskConnectedEntitiesService.createTaskCancellationOfRequestForDisconnectionOfPws(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case DISCONNECTION_OF_POWER_SUPPLY ->
                                taskConnectedEntitiesService.createTaskDisconnectionOfPowerSupply(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case RESCHEDULING ->
                                taskConnectedEntitiesService.createTaskRescheduling(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case LATE_PAYMENT_FINE ->
                                taskConnectedEntitiesService.createTaskLatePaymentFine(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case OBJECTION_TO_CHANGE_OF_CBG ->
                                taskConnectedEntitiesService.createTaskObjectionToChangeOfCbg(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case OBJECTION_WITHDRAWAL_TO_CHANGE_OF_CBG ->
                                taskConnectedEntitiesService.createTaskObjectionWithdrawalToChangeOfCbg(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        case REMINDER_FOR_DISCONNECTION_OF_PWS ->
                                taskConnectedEntitiesService.createTaskReminderForDisconnectionOfPowerSupply(i, task, taskConnectedEntity.getId(), exceptionMessages);
                        default ->
                                throw new ClientException("Unexpected exception handled", ErrorCode.APPLICATION_ERROR);
                    }
                }
            }
            case INTERNAL -> {
                for (int i = 0; i < connectedEntities.size(); i++) {
                    TaskConnectedEntity taskConnectedEntity = connectedEntities.get(i);
                    saveCustomerConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, true);
                    saveContractOrderConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, true);
                    saveCommunicationConnectionTypes(task, exceptionMessages, taskConnectedEntity, i, true);
                }
            }
        }
    }

    /**
     * Saves the communication connection types for a given task. This method processes a
     * specific TaskConnectedEntity object from a list of connected entities based on its entity type
     * and appropriately creates task communication entries.
     *
     * @param task                The Task object for which the communication connection types are being saved.
     * @param exceptionMessages   A list that holds exception messages generated during processing.
     * @param taskConnectedEntity A list of TaskConnectedEntity objects associated with the task.
     * @param i                   The index of the current TaskConnectedEntity in the list to be processed.
     * @throws ClientException if an unsupported entity type is encountered.
     */
    private void saveCommunicationConnectionTypes(Task task, List<String> exceptionMessages, TaskConnectedEntity taskConnectedEntity, int i, boolean forInternalTask) {
        switch (taskConnectedEntity.getEntityType()) {
            case EMAIL_COMMUNICATION, MASS_EMAIL_COMMUNICATION ->
                    taskConnectedEntitiesService.createTaskEmailCommunication(i, task, taskConnectedEntity.getId(), exceptionMessages);
            case SMS_COMMUNICATION ->
                    taskConnectedEntitiesService.createTaskSmsCommunication(i, task, taskConnectedEntity.getId(), exceptionMessages, SmsCommunicationChannel.SMS);
            case MASS_SMS_COMMUNICATION ->
                    taskConnectedEntitiesService.createTaskSmsCommunication(i, task, taskConnectedEntity.getId(), exceptionMessages, SmsCommunicationChannel.MASS_SMS);
            default -> {
                if (!forInternalTask) {
                    throw new ClientException("Unexpected exception handled", ErrorCode.APPLICATION_ERROR);
                }
            }
        }
    }

    /**
     * Saves the contract or order connection types by creating task-related entities based on the provided input.
     *
     * @param task                The Task object for which the connected entities are being processed.
     * @param exceptionMessages   A list to collect any exception messages encountered during entity processing.
     * @param taskConnectedEntity A list of TaskConnectedEntity objects representing the entities to be connected.
     * @param i                   The index of the current TaskConnectedEntity being processed within the connectedEntities list.
     */
    private void saveContractOrderConnectionTypes(Task task, List<String> exceptionMessages, TaskConnectedEntity taskConnectedEntity, int i, boolean forInternalTask) {
        switch (taskConnectedEntity.getEntityType()) {
            case SERVICE_ORDER ->
                    taskConnectedEntitiesService.createTaskServiceOrder(i, task, taskConnectedEntity.getId(), exceptionMessages);
            case GOODS_ORDER ->
                    taskConnectedEntitiesService.createTaskGoodsOrder(i, task, taskConnectedEntity.getId(), exceptionMessages);
            case PRODUCT_CONTRACT ->
                    taskConnectedEntitiesService.createTaskProductContract(i, task, taskConnectedEntity.getId(), exceptionMessages);
            case SERVICE_CONTRACT ->
                    taskConnectedEntitiesService.createTaskServiceContract(i, task, taskConnectedEntity.getId(), exceptionMessages);
            default -> {
                if (!forInternalTask) {
                    throw new ClientException("Unexpected exception handled", ErrorCode.APPLICATION_ERROR);
                }
            }
        }
    }

    /**
     * Saves the customer connection types by processing the specified task connected entity
     * based on its entity type. Depending on the entity type, this method creates either
     * task customers or task customer assessments and appends any exception messages
     * encountered during the process.
     *
     * @param task                the task object associated with the operation
     * @param exceptionMessages   a list to store any exception messages arising during the creation of task customers or assessments
     * @param taskConnectedEntity the list of task connected entities to be processed
     * @param i                   the index of the connected entity to be processed within the connectedEntities list
     */
    private void saveCustomerConnectionTypes(Task task, List<String> exceptionMessages, TaskConnectedEntity taskConnectedEntity, int i, boolean forInternalTask) {
        switch (taskConnectedEntity.getEntityType()) {
            case CUSTOMER ->
                    taskConnectedEntitiesService.createTaskCustomers(i, task, taskConnectedEntity.getId(), exceptionMessages);
            case CUSTOMER_ASSESSMENT ->
                    taskConnectedEntitiesService.createTaskCustomerAssessment(i, task, taskConnectedEntity.getId(), exceptionMessages);
            default -> {
                if (!forInternalTask) {
                    throw new ClientException("Unexpected exception handled", ErrorCode.APPLICATION_ERROR);
                }
            }
        }
    }

    /**
     * Retrieves a task with the specified id.
     *
     * @param id The id of the task to retrieve.
     * @return The task with the specified id.
     * @throws DomainEntityNotFoundException if the task with the presented id is not found.
     */
    private Task getTaskById(Long id) {
        return taskRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Task with the presented id not found"));
    }

    /**
     * Validates the status of a task.
     *
     * @param task The task to be validated.
     * @throws OperationNotAllowedException If the task's status is set to DELETED, an exception is thrown.
     */
    private void validateTaskStatus(Task task) {
        if (task.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("You cannot edit a task with DELETED status");
        }
    }

    /**
     * Saves a new comment for the given task.
     *
     * @param task       the task to save the comment for
     * @param newComment the new comment to be saved
     */
    private void saveNewTaskComment(Task task, String newComment) {
        if (StringUtils.isNotBlank(newComment)) {
            taskCommentRepository.save(new TaskComment(null, newComment, task.getId()));
        }
    }

    /**
     * Adds a comment to the specified task.
     * This method retrieves the task by its ID and then saves the provided comment to that task.
     *
     * @param taskId  the ID of the task to which the comment will be added
     * @param comment the comment to be added to the task
     * @throws DomainEntityNotFoundException if no task with the given ID exists
     * @throws IllegalArgumentException      if the provided comment is null or empty
     */
    public void addCommentToTask(Long taskId, String comment) {
        Task task = getTaskById(taskId);
        saveNewTaskComment(task, comment);
    }

    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return taskActivityService.getActivitiesByConnectedObjectId(id);
    }

    public List<TaskShortResponse> getTasksByProductContractId(Long id) {
        return taskRepository.findProductContractActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByCustomerId(Long id) {
        return taskRepository.findCustomerActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByServiceContractId(Long id) {
        return taskRepository.findServiceContractActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByGoodsOrderId(Long id) {
        return taskRepository.findGoodsOrderActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByServiceOrderId(Long id) {
        return taskRepository.findServiceOrderActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByBillingRunId(Long id) {
        return taskRepository.findBillingRunActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByReceivableBlockingId(Long id) {
        return taskRepository.findReceivableBlockingActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByCustomerAssessmentId(Long id) {
        return taskRepository.findCustomerAssessmentActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByReschedulingId(Long id) {
        return taskRepository.findReschedulingActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByPowerSupplyDisconnectionReminderId(Long id) {
        return taskRepository.findPowerSupplyDisconnectionReminderActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByEmailCommunicationId(Long id) {
        return taskRepository.findEmailCommunicationActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksBySmsCommunicationId(Long id) {
        return taskRepository.findSmsCommunicationActiveTasksSingleSms(id);
    }

    public List<TaskShortResponse> getTasksByMassSmsCommunicationId(Long id) {
        return taskRepository.findSmsCommunicationActiveTasks(id, SmsCommunicationChannel.MASS_SMS);
    }

    public List<TaskShortResponse> getTasksByDisconnectionPowerSupplyRequestId(Long id) {
        return taskRepository.findDisconnectionPowerSupplyRequestActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByObjectionToChangeOfCbg(Long id) {
        return taskRepository.findObjectionToChangeOfCbgActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByObjectionWithdrawalToChangeOfCbg(Long id) {
        return taskRepository.findObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByCancellationId(Long id) {
        return taskRepository.findCancellationActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByDisconnectionOfPowerSupplyId(Long id) {
        return taskRepository.findDisconnectionOfPowerSupplyActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByReconnectionId(Long id) {
        return taskRepository.findReconnectionOfThePowerSupplyActiveTasks(id);
    }

    public List<TaskShortResponse> getTasksByLatePaymentFineId(Long id) {
        return taskRepository.findLatePaymentFineActiveTasks(id);
    }

    public List<GetTermEndDatesResponse> getTermEndDate(GetTermEndDateValuesRequest getTermEndDateValuesRequest) {
        Task tempTask = new Task();

        tempTask.setTaskTypeId(getTermEndDateValuesRequest.getTaskTypeId());

        List<TaskStage> taskStages = getTermEndDateValuesRequest
                .getTaskStageDetailRequestList()
                .stream()
                .map(stageDetail -> {
                            TaskStage taskStage = new TaskStage();
                            taskStage.setStartDate(stageDetail.getStartDate());
                            taskStage.setStage(stageDetail.getStage());
                            taskStage.setTerm(stageDetail.getTerm());
                            taskStage.setTermType(stageDetail.getTermType());
                            return taskStage;
                        }
                )
                .toList();

        taskStages.forEach(stage -> stage.setEndDate(calculateStageEndDate(tempTask, stage, stage.getTerm())));

        return taskStages.stream()
                .map(stage -> new GetTermEndDatesResponse(stage.getStage(), stage.getEndDate()))
                .toList();
    }
}
