package bg.energo.phoenix.service.contract.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.activity.ProductContractActivity;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.contract.activity.ProductContractActivityRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
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
public class ProductContractActivityService extends SystemActivityBaseService {

    private final ProductContractRepository productContractRepository;
    private final ProductContractActivityRepository productContractActivityRepository;

    public ProductContractActivityService(ActivityRepository activityRepository,
                                          SubActivityRepository subActivityRepository,
                                          SystemActivityRepository systemActivityRepository,
                                          NomenclatureService nomenclatureService,
                                          SystemActivityFileService systemActivityFileService,
                                          ProductContractRepository productContractRepository,
                                          ProductContractActivityRepository productContractActivityRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.productContractRepository = productContractRepository;
        this.productContractActivityRepository = productContractActivityRepository;
    }


    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.PRODUCT_CONTRACT;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating an activity for {} with request {};", getActivityConnectionType(), request);

        ProductContract contract = productContractRepository
                .findByIdAndStatusIn(request.getObjectId(), List.of(ProductContractStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "objectId-Product contract not found by ID %s and status in: %s;"
                                .formatted(request.getObjectId(), List.of(ProductContractStatus.ACTIVE)))
                );

        Long systemActivityId = super.create(request, connectionType);

        ProductContractActivity productContractActivity = new ProductContractActivity();
        productContractActivity.setSystemActivityId(systemActivityId);
        productContractActivity.setStatus(EntityStatus.ACTIVE);
        productContractActivity.setContractId(contract.getId());
        productContractActivityRepository.save(productContractActivity);

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
                            permissions = {PRODUCT_CONTRACT_VIEW, PRODUCT_CONTRACT_VIEW_DELETED}
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        ProductContractActivity productContractActivity = productContractActivityRepository
                .findBySystemActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "systemActivityId-Product contract activity not found by ID %s and status in: %s;"
                                .formatted(response.getActivityId(), List.of(EntityStatus.ACTIVE)))
                );

        ProductContract contract = productContractRepository
                .findById(productContractActivity.getContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Product contract not found by ID %s;".formatted(productContractActivity.getContractId())));

        response.setConnectedObjectId(contract.getId());
        response.setConnectedObjectName(contract.getContractNumber());

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for product contract, id {};", id);

        return productContractActivityRepository.findByContractIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }

}
