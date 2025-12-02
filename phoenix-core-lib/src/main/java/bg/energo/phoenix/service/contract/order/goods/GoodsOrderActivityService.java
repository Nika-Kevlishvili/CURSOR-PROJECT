package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderActivityRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
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
public class GoodsOrderActivityService extends SystemActivityBaseService {

    private final GoodsOrderRepository goodsOrderRepository;
    private final GoodsOrderActivityRepository goodsOrderActivityRepository;

    public GoodsOrderActivityService(ActivityRepository activityRepository,
                                     SubActivityRepository subActivityRepository,
                                     SystemActivityRepository systemActivityRepository,
                                     NomenclatureService nomenclatureService,
                                     SystemActivityFileService systemActivityFileService,
                                     GoodsOrderRepository goodsOrderRepository,
                                     GoodsOrderActivityRepository goodsOrderActivityRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.goodsOrderRepository = goodsOrderRepository;
        this.goodsOrderActivityRepository = goodsOrderActivityRepository;
    }


    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.GOODS_ORDER;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);

        GoodsOrder goodsOrder = goodsOrderRepository
                .findByIdAndStatusIn(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "objectId-Goods order not found by ID %s and status in: %s"
                                .formatted(request.getObjectId(), List.of(EntityStatus.ACTIVE))
                ));

        Long systemActivityId = super.create(request, connectionType);

        GoodsOrderActivity goodsOrderActivity = new GoodsOrderActivity();
        goodsOrderActivity.setOrderId(goodsOrder.getId());
        goodsOrderActivity.setSystemActivityId(systemActivityId);
        goodsOrderActivity.setStatus(EntityStatus.ACTIVE);
        goodsOrderActivityRepository.save(goodsOrderActivity);

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
                            context = PermissionContextEnum.GOODS_ORDERS,
                            permissions = {GOODS_ORDER_VIEW, GOODS_ORDER_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        GoodsOrderActivity goodsOrderActivity = goodsOrderActivityRepository
                .findBySystemActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "systemActivityId-Goods order activity not found by ID %s and status in: %s"
                                .formatted(response.getId(), List.of(EntityStatus.ACTIVE))
                ));

        GoodsOrder goodsOrder = goodsOrderRepository
                .findById(goodsOrderActivity.getOrderId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Goods order not found by ID %s;".formatted(goodsOrderActivity.getOrderId())));

        response.setConnectedObjectId(goodsOrder.getId());
        response.setConnectedObjectName(goodsOrder.getOrderNumber());

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for goods order, id {};", id);

        return goodsOrderActivityRepository.findByOrderIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }

}
