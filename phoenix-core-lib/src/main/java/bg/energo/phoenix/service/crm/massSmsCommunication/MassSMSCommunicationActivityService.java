package bg.energo.phoenix.service.crm.massSmsCommunication;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationActivityRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationRepository;
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
public class MassSMSCommunicationActivityService extends SystemActivityBaseService {

    private final SmsCommunicationRepository smsCommunicationRepository;
    private final SmsCommunicationActivityRepository smsCommunicationActivityRepository;

    public MassSMSCommunicationActivityService(ActivityRepository activityRepository, SubActivityRepository subActivityRepository, SystemActivityRepository systemActivityRepository, NomenclatureService nomenclatureService, SystemActivityFileService systemActivityFileService,
                                           SmsCommunicationRepository smsCommunicationRepository, SmsCommunicationActivityRepository smsCommunicationActivityRepository) {
        super(activityRepository, subActivityRepository, systemActivityRepository, nomenclatureService, systemActivityFileService);
        this.smsCommunicationRepository=smsCommunicationRepository;
        this.smsCommunicationActivityRepository=smsCommunicationActivityRepository;
    }

    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);
        SmsCommunication smsCommunication = smsCommunicationRepository
                .findByIdAndStatusAndCommunicationChannel(request.getObjectId(), EntityStatus.ACTIVE,SmsCommunicationChannel.MASS_SMS)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                                "objectId- Mass Sms communication not found by ID %s and status in: %s".formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                        )
                );

        Long systemActivityId = super.create(request, connectionType);

        SmsCommunicationActivity smsCommunicationActivity = new SmsCommunicationActivity();
        smsCommunicationActivity.setSmsCommunicationId(smsCommunication.getId());
        smsCommunicationActivity.setActivityId(systemActivityId);
        smsCommunicationActivity.setStatus(EntityStatus.ACTIVE);
        smsCommunicationActivityRepository.save(smsCommunicationActivity);

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
                            context = PermissionContextEnum.MASS_SMS_COMMUNICATION,
                            permissions = {MASS_SMS_COMMUNICATION_VIEW_DELETED, MASS_SMS_COMMUNICATION_VIEW_DRAFT,MASS_SMS_COMMUNICATION_VIEW_SENT}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        SmsCommunicationActivity smsCommunicationActivity = smsCommunicationActivityRepository
                .findByActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "systemActivityId-sms communication activity not found by ID %s and status in: %s"
                                .formatted(response.getId(), List.of(EntityStatus.ACTIVE))
                ));

        SmsCommunication smsCommunication = smsCommunicationRepository
                .findById(smsCommunicationActivity.getSmsCommunicationId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Sms communication not found by ID %s;".formatted(smsCommunicationActivity.getSmsCommunicationId())));

        response.setConnectedObjectId(smsCommunication.getId());
        response.setConnectedObjectName(String.valueOf(smsCommunication.getSmsSendingNumberId()));

        return response;
    }

    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.MASS_SMS_COMMUNICATION;
    }

    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long connectedObjectId) {
        return smsCommunicationActivityRepository.findBySmsCommunicationIdAndStatus(connectedObjectId, List.of(EntityStatus.ACTIVE),SmsCommunicationChannel.MASS_SMS);
    }
}
