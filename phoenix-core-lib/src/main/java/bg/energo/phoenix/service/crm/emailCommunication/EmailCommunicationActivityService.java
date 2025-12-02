package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationActivityRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.SubActivityRepository;
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
public class EmailCommunicationActivityService extends SystemActivityBaseService {
    private final EmailCommunicationActivityRepository emailCommunicationActivityRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;

    public EmailCommunicationActivityService(ActivityRepository activityRepository,
                                             SubActivityRepository subActivityRepository,
                                             SystemActivityRepository systemActivityRepository,
                                             NomenclatureService nomenclatureService,
                                             SystemActivityFileService systemActivityFileService,
                                             EmailCommunicationRepository emailCommunicationRepository,
                                             EmailCommunicationActivityRepository emailCommunicationActivityRepository
    ) {
        super(activityRepository, subActivityRepository, systemActivityRepository, nomenclatureService, systemActivityFileService);
        this.emailCommunicationActivityRepository = emailCommunicationActivityRepository;
        this.emailCommunicationRepository = emailCommunicationRepository;
    }

    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);

        EmailCommunication emailCommunication = emailCommunicationRepository
                .findByIdAndEntityStatus(request.getObjectId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                                "objectId-Email communication not found by ID %s and status in: %s".formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                        )
                );

        Long systemActivityId = super.create(request, connectionType);

        EmailCommunicationActivity emailCommunicationActivity = new EmailCommunicationActivity();
        emailCommunicationActivity.setEmailCommunicationId(emailCommunication.getId());
        emailCommunicationActivity.setActivityId(systemActivityId);
        emailCommunicationActivity.setStatus(EntityStatus.ACTIVE);
        emailCommunicationActivityRepository.save(emailCommunicationActivity);

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
                            context = PermissionContextEnum.EMAIL_COMMUNICATION,
                            permissions = {EMAIL_COMMUNICATION_VIEW_DRAFT, EMAIL_COMMUNICATION_VIEW_SEND, EMAIL_COMMUNICATION_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        EmailCommunicationActivity emailCommunicationActivity = emailCommunicationActivityRepository
                .findByActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "systemActivityId-Email communication activity not found by ID %s and status in: %s"
                                .formatted(response.getId(), List.of(EntityStatus.ACTIVE))
                ));

        EmailCommunication emailCommunication = emailCommunicationRepository
                .findById(emailCommunicationActivity.getEmailCommunicationId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Email communication not found by ID %s;".formatted(emailCommunicationActivity.getEmailCommunicationId())));

        response.setConnectedObjectId(emailCommunication.getId());
        response.setConnectedObjectName(emailCommunication.getDmsNumber());

        return response;
    }

    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.EMAIL_COMMUNICATION;
    }

    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for email communication, id {};", id);
        return emailCommunicationActivityRepository.findByEmailCommunicationIdAndStatus(id, List.of(EntityStatus.ACTIVE));
    }
}
