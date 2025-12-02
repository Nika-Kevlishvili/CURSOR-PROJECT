package bg.energo.phoenix.service.nomenclature.mapper;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskTypeStage;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.request.nomenclature.contract.TaskTypeBaseRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.TaskTypeStageRequest;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerShortResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeStageResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskTypeMapperService {
    public TaskTypeStage mapToTaskTypeStage(Long taskTypeId, TaskTypeStageRequest taskTypeStageRequest) {
        TaskTypeStage taskTypeStage = new TaskTypeStage();
        taskTypeStage.setTaskTypeId(taskTypeId);
        taskTypeStage.setStage(taskTypeStageRequest.getStage());
        PerformerType performerType = taskTypeStageRequest.getPerformerType();
        if(PerformerType.MANAGER.equals(performerType)){
            taskTypeStage.setPerformerId(taskTypeStageRequest.getPerformerId());
            taskTypeStage.setPerformerType(PerformerType.MANAGER);
        }else if (PerformerType.TAG.equals(performerType)){
            taskTypeStage.setPerformerGroupId(taskTypeStageRequest.getPerformerId());
            taskTypeStage.setPerformerType(PerformerType.TAG);
        }
        taskTypeStage.setTermType(taskTypeStageRequest.getTermType());
        taskTypeStage.setTerm(taskTypeStageRequest.getTerm());
        taskTypeStage.setStatus(EntityStatus.ACTIVE);
        return taskTypeStage;
    }

    public TaskType mapToTaskType(TaskTypeBaseRequest request, Long orderingId) {
        TaskType taskType = new TaskType();
        taskType.setName(request.getName());
        taskType.setCalendarId(request.getCalendarId());
        taskType.setIsDefault(request.getDefaultSelection());
        taskType.setStatus(request.getStatus());
        taskType.setOrderingId(orderingId);
        return taskType;
    }

    public TaskTypeStageResponse mapToTaskTypeStageResponse(TaskTypeStage taskTypeStage, Optional<AccountManager> accountManager, PortalTagResponse tagResponse) {
        TaskTypeStageResponse taskTypeStageResponse = new TaskTypeStageResponse();
        taskTypeStageResponse.setId(taskTypeStage.getId());
        taskTypeStageResponse.setStage(taskTypeStage.getStage());
        taskTypeStageResponse.setPerformer(accountManager.map(AccountManagerShortResponse::new).orElse(null));
        taskTypeStageResponse.setTagPerformer(tagResponse);
        taskTypeStageResponse.setPerformerType(taskTypeStage.getPerformerType());
        taskTypeStageResponse.setTerm(taskTypeStage.getTerm());
        taskTypeStageResponse.setTermType(taskTypeStage.getTermType());
        return taskTypeStageResponse;
    }

    public TaskTypeResponse mapToTaskTypeResponse(TaskType taskType, CalendarShortResponse calendar, List<TaskTypeStageResponse> taskTypeStageResponse) {
        TaskTypeResponse taskTypeResponse = new TaskTypeResponse();
        taskTypeResponse.setId(taskType.getId());
        taskTypeResponse.setName(taskType.getName());
        taskTypeResponse.setCalendar(calendar);
        taskTypeResponse.setTaskTypeStages(taskTypeStageResponse);
        taskTypeResponse.setStatus(taskType.getStatus());
        taskTypeResponse.setSystemUserId(taskType.getSystemUserId());
        taskTypeResponse.setOrderingId(taskType.getOrderingId());
        taskTypeResponse.setDefaultSelection(taskType.getIsDefault());
        return taskTypeResponse;
    }
}
