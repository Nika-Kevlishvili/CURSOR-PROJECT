package bg.energo.phoenix.service.customer.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.activity.CustomerActivity;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.activity.CustomerActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.SubActivityRepository;
import bg.energo.phoenix.service.activity.SystemActivityBaseService;
import bg.energo.phoenix.service.activity.SystemActivityFileService;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
public class CustomerActivityService extends SystemActivityBaseService {

    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerActivityRepository customerActivityRepository;

    public CustomerActivityService(ActivityRepository activityRepository,
                                   SubActivityRepository subActivityRepository,
                                   SystemActivityRepository systemActivityRepository,
                                   NomenclatureService nomenclatureService,
                                   SystemActivityFileService systemActivityFileService,
                                   CustomerRepository customerRepository,
                                   CustomerActivityRepository customerActivityRepository,
                                   CustomerDetailsRepository customerDetailsRepository) {
        super(
                activityRepository,
                subActivityRepository,
                systemActivityRepository,
                nomenclatureService,
                systemActivityFileService
        );
        this.customerRepository = customerRepository;
        this.customerActivityRepository = customerActivityRepository;
        this.customerDetailsRepository = customerDetailsRepository;
    }


    @Override
    public SystemActivityConnectionType getActivityConnectionType() {
        return SystemActivityConnectionType.CUSTOMER;
    }


    @Override
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating activity for {} with request {};", getActivityConnectionType(), request);

        Customer customer = customerRepository
                .findByIdAndStatuses(request.getObjectId(), List.of(CustomerStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("objectId-Customer not found by ID %s in status in %s;"
                        .formatted(request.getObjectId(), List.of(CustomerStatus.ACTIVE))));

        Long systemActivityId = super.create(request, connectionType);

        CustomerActivity customerActivity = new CustomerActivity();
        customerActivity.setCustomerId(customer.getId());
        customerActivity.setSystemActivityId(systemActivityId);
        customerActivity.setStatus(EntityStatus.ACTIVE);
        customerActivityRepository.save(customerActivity);

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
                            context = PermissionContextEnum.CUSTOMER,
                            permissions = {
                                    CUSTOMER_VIEW_BASIC,
                                    CUSTOMER_VIEW_DELETED,
                                    CUSTOMER_VIEW_GDPR,
                                    CUSTOMER_VIEW_GDPR_AM,
                                    CUSTOMER_VIEW_BASIC_AM
                            }
                    )
            }
    )
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing an activity for {} with ID {};", getActivityConnectionType(), id);

        SystemActivityResponse response = super.view(id);

        CustomerActivity customerActivity = customerActivityRepository
                .findBySystemActivityIdAndStatusIn(response.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("systemActivityId-CustomerActivity not found by ID %s in status %s;"
                        .formatted(response.getId(), List.of(EntityStatus.ACTIVE))));

        CustomerDetails customerDetails = customerDetailsRepository
                .findFirstByCustomerId(customerActivity.getCustomerId(), Sort.by(Sort.Direction.DESC, "createDate"))
                .orElseThrow(() -> new DomainEntityNotFoundException("customerId-CustomerDetails not found by ID %s;"
                        .formatted(customerActivity.getCustomerId())));

        response.setConnectedObjectId(customerDetails.getCustomerId());
        response.setConnectedObjectName(
                String.format(
                        "%s %s %s (%s)",
                        customerDetails.getName(),
                        StringUtils.isNotEmpty(customerDetails.getMiddleName()) ? customerDetails.getMiddleName() : "",
                        StringUtils.isNotEmpty(customerDetails.getLastName()) ? customerDetails.getLastName() : "",
                        customerDetails.getCustomerId().toString()
                ).trim()
        );

        return response;
    }


    @Override
    public List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long id) {
        log.debug("Viewing activities for customer, id {};", id);

        return customerActivityRepository.findByCustomerIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
    }

}
