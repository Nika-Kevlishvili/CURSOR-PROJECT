package bg.energo.phoenix.service.contract.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.activity.ServiceContractActivity;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.contract.activity.ServiceContractActivityRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
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
public class ServiceContractActivityService extends SystemActivityBaseService {

    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractActivityRepository serviceContractActivityRepository;

    public ServiceContractActivityService(ActivityRepository activityRepository,
                                          SubActivityRepository subActivityRepository,
                                          SystemActivityRepository systemActivityRepository,
                                          NomenclatureService nomenclatureService,
                                          SystemActivityFileService systemActivityFileService,
                                          ServiceContractsRepository serviceContractsRepository,
                                          ServiceContractActivityRepository serviceContractActivityRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.serviceContractsRepository = serviceContractsRepository;
        this.serviceContractActivityRepository = serviceContractActivityRepository;
    }


    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.SERVICE_CONTRACT;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);

        ServiceContracts serviceContract = serviceContractsRepository
                .findByIdAndStatusIn(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "objectId-Service contract not found by ID %s and status in: %s;"
                                .formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE)))
                );

        Long systemActivityId = super.create(request, connectionType);

        ServiceContractActivity serviceContractActivity = new ServiceContractActivity();
        serviceContractActivity.setSystemActivityId(systemActivityId);
        serviceContractActivity.setStatus(EntityStatus.ACTIVE);
        serviceContractActivity.setContractId(serviceContract.getId());
        serviceContractActivityRepository.save(serviceContractActivity);

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
                            context = PermissionContextEnum.PRODUCT_CONTRACTS,
                            permissions = {SERVICE_CONTRACT_VIEW, SERVICE_CONTRACT_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        ServiceContractActivity serviceContractActivity = serviceContractActivityRepository
                .findBySystemActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "objectId-Service contract activity not found by ID %s and status in: %s;"
                                .formatted(response.getActivityId(), List.of(EntityStatus.ACTIVE)))
                );

        ServiceContracts contract = serviceContractsRepository
                .findById(serviceContractActivity.getContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Service contract not found by ID %s;".formatted(serviceContractActivity.getContractId())));

        response.setConnectedObjectId(contract.getId());
        response.setConnectedObjectName(contract.getContractNumber());

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for service contract, id {};", id);

        return serviceContractActivityRepository.findByContractIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }

}
