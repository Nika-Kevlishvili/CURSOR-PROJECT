package bg.energo.phoenix.service.task;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.entity.task.TaskStage;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.enums.task.TaskStageStatus;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.task.TaskStageRepository;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.service.NotificationModel;
import bg.energo.phoenix.service.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStateUpdaterService {
    private final TaskRepository taskRepository;
    private final TaskStageRepository taskStageRepository;
    private final PortalTagRepository portalTagRepository;
    private final NotificationService notificationService;

    @ExecutionTimeLogger
    public void update() {
        log.debug("Starting update for overdue tasks");
        List<Task> allOverdueTasks = taskRepository.findAllOverdueTasks(LocalDate.now());
        allOverdueTasks.forEach(task -> {
            try {
                task.setTaskStatus(TaskStatus.OVERDUE);
                TaskStage taskCurrentStage = taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1));
                taskCurrentStage.setTaskStageStatus(TaskStageStatus.OVERDUE);
                taskStageRepository.save(taskCurrentStage);
                taskRepository.save(task);
            } catch (Exception e) {
                log.error("Some error happened when working to update overdue task with id: %s, skipping current task".formatted(task.getId()), e);
            }
        });
    }

    private void sendOverdueNotifications(TaskStage taskStage, Long taskId, NotificationType type) {
        PerformerType performerType = taskStage.getPerformerType();
        List<Long> managerIds = switch (performerType) {
            case TAG -> portalTagRepository.findManagerIdsByTagId(taskStage.getPerformer(), EntityStatus.ACTIVE);
            case MANAGER -> taskStage.getPerformer() == null ? List.of() : List.of(taskStage.getPerformer());
        };
        notificationService.sendNotifications(managerIds.stream().map(x -> new NotificationModel(x, taskId, type)).toList());
    }

    public void findExpiredJobs() {
        List<TaskStage> allExpiredTasks = taskRepository.findAllExpiredTasks(LocalDate.now());
        for (TaskStage allExpiredTask : allExpiredTasks) {
            if (allExpiredTask.getPerformerType() != null) {
                sendOverdueNotifications(allExpiredTask, allExpiredTask.getTaskId(), NotificationType.TASK_EXPIRATION_PERFORMER_NOT_EXISTS);
            }
        }
    }

    public void overdueEveryDay() {
        List<Task> allTaskByStatus = taskRepository.findAllTaskByStatus(TaskStatus.OVERDUE);
        for (Task task : allTaskByStatus) {
            TaskStage taskCurrentStage = taskStageRepository.findTaskCurrentStage(task.getId(), PageRequest.of(0, 1));
            if (taskCurrentStage.getPerformerType() != null) {
                sendOverdueNotifications(taskCurrentStage, task.getId(), NotificationType.TASK_OVERDUE_STAGE_PERFORMERS);
            }
        }
    }
}
