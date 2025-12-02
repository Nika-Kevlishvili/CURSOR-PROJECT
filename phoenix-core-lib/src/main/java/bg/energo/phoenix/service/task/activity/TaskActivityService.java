package bg.energo.phoenix.service.task.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.entity.task.TaskActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.activity.TaskActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.SubActivityRepository;
import bg.energo.phoenix.repository.task.TaskActivityRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.service.activity.SystemActivityBaseService;
import bg.energo.phoenix.service.activity.SystemActivityFileService;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
public class TaskActivityService extends SystemActivityBaseService {

    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;

    public TaskActivityService(ActivityRepository activityRepository,
                               SubActivityRepository subActivityRepository,
                               SystemActivityRepository systemActivityRepository,
                               NomenclatureService nomenclatureService,
                               SystemActivityFileService systemActivityFileService,
                               TaskRepository taskRepository,
                               TaskActivityRepository taskActivityRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.taskRepository = taskRepository;
        this.taskActivityRepository = taskActivityRepository;
    }

    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.TASK;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating activity for {} with request {};", getActivityConnectionType(), request);

        Task task = taskRepository
                .findByIdAndStatusIn(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Task not found by ID %s and status in: %s;"
                                                                             .formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE))));

        Long systemActivityId = super.create(request, connectionType);

        TaskActivity taskActivity = new TaskActivity();
        taskActivity.setTaskId(task.getId());
        taskActivity.setSystemActivityId(systemActivityId);
        taskActivity.setStatus(EntityStatus.ACTIVE);
        taskActivityRepository.save(taskActivity);

        return systemActivityId;
    }


    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.SYSTEM_ACTIVITIES,
                            permissions = {SYSTEM_ACTIVITY_VIEW_BASIC, SYSTEM_ACTIVITY_VIEW_DELETED}
                    ),
                    @PermissionMapping(
                            context = PermissionContextEnum.TASK,
                            permissions = {TASK_VIEW_BASIC, TASK_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        TaskActivityShortResponse taskActivityShortResponse = taskActivityRepository
                .getTaskActivityShortResponse(response.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Task information not found for activity ID %s;".formatted(response.getId())));

        response.setConnectedObjectId(taskActivityShortResponse.getTaskId());
        response.setConnectedObjectName(taskActivityShortResponse.getName());

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for task, id {};", id);

        return taskActivityRepository.findByTaskIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }

}
