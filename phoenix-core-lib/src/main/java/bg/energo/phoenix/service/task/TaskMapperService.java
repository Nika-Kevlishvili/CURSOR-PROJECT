package bg.energo.phoenix.service.task;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskTypeStage;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.entity.task.TaskComment;
import bg.energo.phoenix.model.entity.task.TaskStage;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.enums.task.TaskStageStatus;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import bg.energo.phoenix.model.request.task.CreateTaskRequest;
import bg.energo.phoenix.model.request.task.TaskPerformerRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerShortResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeShortResponse;
import bg.energo.phoenix.model.response.task.TaskCommentHistory;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import bg.energo.phoenix.model.response.task.TaskResponse;
import bg.energo.phoenix.model.response.task.TaskStageResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This class provides methods for mapping data between different representations of a Task.
 */
@Service
public class TaskMapperService {
    private final PortalTagRepository portalTagRepository;

    public TaskMapperService(PortalTagRepository portalTagRepository) {
        this.portalTagRepository = portalTagRepository;
    }

    public Task mapFromRequestToTaskEntity(Long id, CreateTaskRequest request) {
        return Task
                .builder()
                .id(id)
                .number(id)
                .taskStatus(TaskStatus.OPEN)
                .status(EntityStatus.ACTIVE)
                .connectionType(request.getConnectionType())
                .taskTypeId(request.getTaskTypeId())
                .description(request.getDescription())
                .build();
    }

    public List<TaskStage> mapFromRequestToTaskStagesEntity(Long taskId, List<TaskTypeStage> taskTypeStages, List<TaskPerformerRequest> taskPerformerRequests) {
        return taskPerformerRequests.stream().map(taskPerformerRequest ->
                TaskStage
                        .builder()
                        .taskId(taskId)
                        .stage(taskPerformerRequest.getStage())
                        .taskTypeStageId(taskTypeStages.stream().filter(taskTypeStage -> taskTypeStage.getStage().equals(taskPerformerRequest.getStage())).findFirst().get().getId())
                        .taskStageStatus(TaskStageStatus.OPEN)
                        .performer(PerformerType.TAG.equals(taskPerformerRequest.getPerformerType()) ? null:taskPerformerRequest.getPerformer())
                        .performerGroup(PerformerType.TAG.equals(taskPerformerRequest.getPerformerType()) ? taskPerformerRequest.getPerformer():null)
                        .performerType(taskPerformerRequest.getPerformerType())
                        .term(taskPerformerRequest.getTerm())
                        .termType(taskTypeStages.stream().filter(taskTypeStage -> taskTypeStage.getStage().equals(taskPerformerRequest.getStage())).findFirst().get().getTermType())
                        .startDate(taskPerformerRequest.getStartDate())
                        .endDate(null)
                        .build()).toList();
    }

    public TaskResponse mapFromEntityToResponse(Task task,
                                                TaskType taskType,
                                                List<TaskStageResponse> taskStages,
                                                List<TaskCommentHistory> commentHistory,
                                                List<TaskConnectedEntityResponse> taskConnectedEntitiesResponse,
                                                List<SystemActivityShortResponse> activities) {
        String currentPerformer = "";
        String currentPerformerUsername = "";
        Integer currentTaskStageNumber = 1;
        Optional<TaskStageResponse> currentStage = taskStages
                .stream()
                .filter(taskStageResponse -> taskStageResponse.getCompleteDate() == null)
                .min(Comparator.comparing(TaskStageResponse::getStage));
        if (currentStage.isPresent()) {
            TaskStageResponse currentTaskStage = currentStage.get();
            currentTaskStageNumber = currentTaskStage.getStage();
            if (!task.getTaskStatus().equals(TaskStatus.COMPLETED)) {
                if (isCurrentDateInTaskStage(currentTaskStage)) {
                    if (Objects.nonNull(currentTaskStage.getCurrentPerformer())) {
                        currentPerformer = currentTaskStage.getCurrentPerformer().name();
                        currentPerformerUsername = currentTaskStage.getCurrentPerformer().username();
                        currentTaskStageNumber = currentTaskStage.getStage();
                    }
                }
            }
        } else {
            Optional<TaskStageResponse> lastStageOptional = taskStages.stream().max(Comparator.comparing(TaskStageResponse::getStage));
            if (lastStageOptional.isPresent()) {
                TaskStageResponse lastStage = lastStageOptional.get();
                currentTaskStageNumber = lastStage.getStage();
                if (!task.getTaskStatus().equals(TaskStatus.COMPLETED)) {
                    if (Objects.nonNull(lastStage.getCurrentPerformer())) {
                        currentPerformer = lastStage.getCurrentPerformer().name();
                        currentPerformerUsername = lastStage.getCurrentPerformer().username();
                    }
                }
            }
        }

        return TaskResponse
                .builder()
                .id(task.getId())
                .number(task.getNumber())
                .currentPerformer(currentPerformer)
                .currentPerformerUsername(currentPerformerUsername)
                .currentTaskStageNumber(currentTaskStageNumber)
                .taskType(new TaskTypeShortResponse(taskType))
                .taskStatus(task.getTaskStatus())
                .status(task.getStatus())
                .connectionType(task.getConnectionType())
                .description(task.getDescription())
                .commentHistory(commentHistory)
                .connectedEntities(taskConnectedEntitiesResponse)
                .taskStages(taskStages)
                .activities(activities)
                .build();
    }

    public List<TaskStageResponse> mergeTaskStagesWithPerformers(List<TaskStage> taskStages, AccountManagerRepository accountManagerRepository) {
        return taskStages
                .stream()
                .map(taskStage -> TaskStageResponse.builder()
                        .id(taskStage.getId())
                        .performer(taskStage.getPerformer()==null ? null :
                                new AccountManagerShortResponse(accountManagerRepository.findById(taskStage.getPerformer())
                                        .orElseThrow(() -> new DomainEntityNotFoundException("Account Manager with id: [%s] not found that assigned to Task Stage with id: [%s]".formatted(taskStage.getPerformer(), taskStage.getId()))))
                        )
                        .tagPerformer(taskStage.getPerformerGroup()==null ?
                                null:
                                new PortalTagResponse(portalTagRepository.findById(taskStage.getPerformerGroup())
                                        .orElseThrow(()->new DomainEntityNotFoundException("Portal tag was not found!:")))
                                )
                        .currentPerformer(taskStage.getCurrentPerformerId()==null ? null :
                                new AccountManagerShortResponse(accountManagerRepository.findById(taskStage.getCurrentPerformerId())
                                        .orElseThrow(() -> new DomainEntityNotFoundException("Account Manager with id: [%s] not found that assigned to Task Stage with id: [%s]".formatted(taskStage.getPerformer(), taskStage.getId()))))
                        )
                        .term(taskStage.getTerm())
                        .performerType(taskStage.getPerformerType())
                        .termType(taskStage.getTermType())
                        .stage(taskStage.getStage())
                        .status(taskStage.getTaskStageStatus())
                        .startDate(taskStage.getStartDate())
                        .endDate(taskStage.getEndDate())
                        .completeDate(taskStage.getCompletionDate())
                        .build()
                )
                .toList();
    }

    /**
     * Checks if the current date is within the range of the given task stage.
     *
     * @param currentTaskStage the current task stage
     * @return true if the current date is within the range of the task stage, false otherwise
     */
    private boolean isCurrentDateInTaskStage(TaskStageResponse currentTaskStage) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = currentTaskStage.getStartDate();
        LocalDate endDate = currentTaskStage.getEndDate();

        if (Objects.isNull(startDate)) {
            return false;
        }

        if (Objects.isNull(endDate)) {
            return !startDate.isBefore(now);
        }

        return EPBDateUtils.isDateInRange(now, startDate, endDate);
    }

    public String mapTaskCommentResponse(List<TaskComment> taskComments, AccountManagerRepository accountManagerRepository) {
        StringBuilder commentHistoryBuilder = new StringBuilder();
        taskComments.forEach(taskComment -> {
            String commenter = taskComment.getSystemUserId();
            Optional<AccountManager> commenterOptional = accountManagerRepository.findByUserName(taskComment.getSystemUserId());
            if (commenterOptional.isPresent()) {
                commenter = commenterOptional.get().getDisplayName();
            }
            commentHistoryBuilder.append("[%s - %s]: %s".formatted(commenter, taskComment.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")), taskComment.getComment())).append(";\n");
        });

        return commentHistoryBuilder.toString();
    }
    public List<TaskCommentHistory> mapTaskCommentHistoryResponse(List<TaskComment> taskComments, AccountManagerRepository accountManagerRepository) {
        List<TaskCommentHistory> taskCommentHistories = new ArrayList<>();
        taskComments.forEach(taskComment -> {
            String commenter = taskComment.getSystemUserId();
            Optional<AccountManager> commenterOptional = accountManagerRepository.findByUserName(taskComment.getSystemUserId());
            if (commenterOptional.isPresent()) {
                commenter = commenterOptional.get().getDisplayName();
            }
            TaskCommentHistory taskCommentHistory = new TaskCommentHistory();
            taskCommentHistory.setCommenter(commenter);
            taskCommentHistory.setCreateDate(taskComment.getCreateDate());
            taskCommentHistory.setComment(taskComment.getComment());
            taskCommentHistories.add(taskCommentHistory);
            //commentHistoryBuilder.append("[%s - %s]: %s".formatted(commenter, taskComment.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")), taskComment.getComment())).append(";\n");
        });

        return taskCommentHistories;
    }
}
