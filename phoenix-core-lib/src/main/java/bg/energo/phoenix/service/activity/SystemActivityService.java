package bg.energo.phoenix.service.activity;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.activity.ActivityListColumns;
import bg.energo.phoenix.model.enums.activity.ActivitySearchFields;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.request.activity.ActivityListRequest;
import bg.energo.phoenix.model.request.activity.BaseSystemActivityRequest;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityListResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.permissions.PermissionContextEnum.SYSTEM_ACTIVITIES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemActivityService {

    private final SystemActivityRepository systemActivityRepository;
    private final PermissionService permissionService;
    private final List<SystemActivityBaseService> systemActivityBaseServiceList;


    /**
     * Delegates creating an activity for the given connection type with the given request.
     *
     * @param connectionType the connection type to create the activity for
     * @param request        the request to create the activity with
     * @return the ID of the created activity
     */
    public Long createActivity(SystemActivityConnectionType connectionType, CreateSystemActivityRequest request) {
        List<String> context = new ArrayList<>(permissionService.getPermissionsFromContext(SYSTEM_ACTIVITIES));

        if (connectionType.equals(SystemActivityConnectionType.PRODUCT_CONTRACT) || connectionType.equals(SystemActivityConnectionType.SERVICE_CONTRACT)) {
            if (context.stream().noneMatch(x -> List.of(CREATE_ACTIVITY_FROM_CONTRACT_PREVIEW.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        if (connectionType.equals(SystemActivityConnectionType.SERVICE_ORDER) || connectionType.equals(SystemActivityConnectionType.GOODS_ORDER)) {
            if (context.stream().noneMatch(x -> List.of(CREATE_ACTIVITY_FROM_ORDER_PREVIEW.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        if (connectionType.equals(SystemActivityConnectionType.TASK)) {
            if (context.stream().noneMatch(x -> List.of(CREATE_ACTIVITY_FROM_TASK_PREVIEW.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        if (connectionType.equals(SystemActivityConnectionType.EMAIL_COMMUNICATION)) {
            if (context.stream().noneMatch(x -> List.of(EMAIL_COMMUNICATION_CREATE_ACTIVITY.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        if (connectionType.equals(SystemActivityConnectionType.MASS_EMAIL_COMMUNICATION)) {
            if (context.stream().noneMatch(x -> List.of(MASS_EMAIL_COMMUNICATION_CREATE_ACTIVITY.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        if (connectionType.equals(SystemActivityConnectionType.SMS_COMMUNICATION)) {
            if (context.stream().noneMatch(x -> List.of(SMS_COMMUNICATION_CREATE_ACTIVITY.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }


        if (connectionType.equals(SystemActivityConnectionType.MASS_SMS_COMMUNICATION)) {
            if (context.stream().noneMatch(x -> List.of(MASS_SMS_COMMUNICATION_CREATE_ACTIVITY.getId(), SYSTEM_ACTIVITY_CREATE.getId()).contains(x))) {
                log.error("You does not have permission to create activities for {};", connectionType.name());
                throw new ClientException("You does not have permission to create task for %s;".formatted(connectionType.name()), ErrorCode.ACCESS_DENIED);
            }
        }

        return findActivityBaseService(connectionType).create(request, connectionType);
    }


    /**
     * Delegates editing an activity for the given connection type with the given ID and request.
     *
     * @param id      the ID of the activity to edit
     * @param request the request to edit the activity with
     * @return the ID of the edited activity
     */
    public Long editActivity(Long id, BaseSystemActivityRequest request) {
        return findActivityBaseService(getSystemActivityConnectionType(id)).edit(id, request);
    }


    /**
     * Delegates returning a preview of an activity for the given connection type with the given ID.
     *
     * @param id the ID of the activity to view
     * @return a preview of the activity
     */
    public SystemActivityResponse view(Long id) {
        return findActivityBaseService(getSystemActivityConnectionType(id)).view(id);
    }


    /**
     * Delegates deleting an activity for the given connection type with the given ID.
     *
     * @param id             the ID of the activity to delete
     * @return the ID of the deleted activity
     */
    public Long delete(Long id) {
        return findActivityBaseService(getSystemActivityConnectionType(id)).delete(id);
    }


    /**
     * Finds and returns the {@link SystemActivityBaseService} for the given activity connection type.
     *
     * @param systemActivityConnectionType the activity connection type to find the service for
     * @return the {@link SystemActivityBaseService} for the given activity connection type
     */
    private SystemActivityBaseService findActivityBaseService(SystemActivityConnectionType systemActivityConnectionType) {
        Optional<SystemActivityBaseService> activityBaseServiceOptional = systemActivityBaseServiceList
                .stream()
                .filter(service -> service.getActivityConnectionType().equals(systemActivityConnectionType))
                .findFirst();

        if (activityBaseServiceOptional.isEmpty()) {
            log.error("Service does not exist for activity connection type: {}", systemActivityConnectionType);
            throw new DomainEntityNotFoundException("Service does not exist for activity connection type: %s".formatted(systemActivityConnectionType));
        }

        return activityBaseServiceOptional.get();
    }


    /**
     * @param id the ID of the activity to return the connection type for
     * @return the connection type of the activity with the given ID
     */
    private SystemActivityConnectionType getSystemActivityConnectionType(Long id) {
        return systemActivityRepository
                .findConnectionTypeById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity connection type not found by ID %s;".formatted(id)));
    }


    /**
     * Returns a page of {@link SystemActivityListResponse} objects for the given {@link ActivityListRequest}.
     *
     * @param request the {@link ActivityListRequest} to filter the activities by
     * @return a page of {@link SystemActivityListResponse} objects
     */
    public Page<SystemActivityListResponse> list(ActivityListRequest request) {
        log.debug("Filtering activities by: {}", request.toString());

        return systemActivityRepository.list(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchByField(request),
                getActivityStatuses(),
                request.getSubActivityIds(),
                getConnectionTypes(request),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(new Sort.Order(request.getDirection(), getSortByField(request)))
                )
        );
    }


    /**
     * @return a list of {@link EntityStatus} objects for the given {@link ActivityListRequest} based on permissions
     */
    private List<EntityStatus> getActivityStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(SYSTEM_ACTIVITIES, List.of(SYSTEM_ACTIVITY_VIEW_BASIC))) {
            statuses.add(EntityStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(SYSTEM_ACTIVITIES, List.of(SYSTEM_ACTIVITY_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }

        return statuses;
    }


    /**
     * Returns a list of {@link SystemActivityConnectionType} objects for the given {@link ActivityListRequest}.
     *
     * @param request the {@link ActivityListRequest} to filter the activities by
     * @return a list of {@link SystemActivityConnectionType} objects if the request contains connection types, otherwise a list of all connection types
     */
    private List<SystemActivityConnectionType> getConnectionTypes(ActivityListRequest request) {
        if (CollectionUtils.isEmpty(request.getConnectionTypes())) {
            return null;
        } else {
            List<SystemActivityConnectionType> connectionTypes = new ArrayList<>();
            for (String connectionType : request.getConnectionTypes()) {
                // "CONTRACT" and "ORDER" is separated on DB level, but on UI level they are the same
                if (connectionType.equals("CONTRACT")) {
                    connectionTypes.add(SystemActivityConnectionType.SERVICE_CONTRACT);
                    connectionTypes.add(SystemActivityConnectionType.PRODUCT_CONTRACT);
                } else if (connectionType.equals("ORDER")) {
                    connectionTypes.add(SystemActivityConnectionType.GOODS_ORDER);
                    connectionTypes.add(SystemActivityConnectionType.SERVICE_ORDER);
                } else {
                    connectionTypes.add(SystemActivityConnectionType.valueOf(connectionType));
                }
            }
            return connectionTypes;
        }
    }


    /**
     * Returns the search field for the given {@link ActivityListRequest}, in which the prompt should be searched.
     *
     * @param request the {@link ActivityListRequest} to filter the activities by
     * @return the search field for the given {@link ActivityListRequest}
     */
    private String getSearchByField(ActivityListRequest request) {
        return Optional.ofNullable(request.getSearchBy())
                .map(Enum::name)
                .orElse(ActivitySearchFields.ALL.name());
    }


    /**
     * Returns the sort field for the given {@link ActivityListRequest}.
     *
     * @param request the {@link ActivityListRequest} to filter the activities by
     * @return the sort field for the given {@link ActivityListRequest}
     */
    private String getSortByField(ActivityListRequest request) {
        return Optional.ofNullable(request.getSortBy())
                .map(ActivityListColumns::getValue)
                .orElse(ActivityListColumns.ID.getValue());
    }

}
