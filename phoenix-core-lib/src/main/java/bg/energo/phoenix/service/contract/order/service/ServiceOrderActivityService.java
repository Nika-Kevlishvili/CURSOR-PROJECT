package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderActivityRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
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
public class ServiceOrderActivityService extends SystemActivityBaseService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderActivityRepository serviceOrderActivityRepository;


    public ServiceOrderActivityService(ActivityRepository activityRepository,
                                       SubActivityRepository subActivityRepository,
                                       SystemActivityRepository systemActivityRepository,
                                       NomenclatureService nomenclatureService,
                                       SystemActivityFileService systemActivityFileService,
                                       ServiceOrderRepository serviceOrderRepository,
                                       ServiceOrderActivityRepository serviceOrderActivityRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.serviceOrderRepository = serviceOrderRepository;
        this.serviceOrderActivityRepository = serviceOrderActivityRepository;
    }


    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.SERVICE_ORDER;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);

        ServiceOrder serviceOrder = serviceOrderRepository
                .findByIdAndStatusIn(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "objectId-Service order not found by ID %s and status in: %s;"
                                .formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                ));

        Long systemActivityId = super.create(request, connectionType);

        ServiceOrderActivity serviceOrderActivity = new ServiceOrderActivity();
        serviceOrderActivity.setOrderId(serviceOrder.getId());
        serviceOrderActivity.setSystemActivityId(systemActivityId);
        serviceOrderActivity.setStatus(EntityStatus.ACTIVE);
        serviceOrderActivityRepository.save(serviceOrderActivity);

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
                            context = PermissionContextEnum.SERVICE_ORDERS,
                            permissions = {SERVICE_ORDER_VIEW, SERVICE_ORDER_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        ServiceOrderActivity serviceOrderActivity = serviceOrderActivityRepository
                .findBySystemActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "systemActivityId-Service order activity not found by ID %s and status in: %s;"
                                .formatted(response.getId(), List.of(EntityStatus.ACTIVE))
                ));

        ServiceOrder serviceOrder = serviceOrderRepository
                .findById(serviceOrderActivity.getOrderId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Service order not found by ID %s;".formatted(serviceOrderActivity.getOrderId())));

        response.setConnectedObjectId(serviceOrder.getId());
        response.setConnectedObjectName(serviceOrder.getOrderNumber());

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for service order, id {};", id);

        return serviceOrderActivityRepository.findByOrderIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }
}
