package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.ActivityRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ActivityResponse;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.ACTIVITIES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService implements NomenclatureBaseService {
    private final ActivityRepository activityRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ACTIVITIES;
    }

    /**
     * Filters {@link Activity} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Activity}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTIVITIES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering activities nomenclature with request: {}", request.toString());
        return activityRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Activity} item in the activites list to a specified position.
     * The method retrieves the activity item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the activity item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link Activity} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTIVITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of activity item with ID: {} in activities to place: {}", request.getId(), request.getOrderingId());

        Activity activity = activityRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Activity> activities;

        if (activity.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = activity.getOrderingId();

            activities = activityRepository.findInOrderingIdRange(
                    start,
                    end,
                    activity.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Activity c : activities) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = activity.getOrderingId();
            end = request.getOrderingId();

            activities = activityRepository.findInOrderingIdRange(
                    start,
                    end,
                    activity.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Activity c : activities) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        activity.setOrderingId(request.getOrderingId());
        activities.add(activity);
        activityRepository.saveAll(activities);
    }

    /**
     * Sorts all {@link Activity} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTIVITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the activities alphabetically");
        List<Activity> activities = activityRepository.orderByName();
        long orderingId = 1;

        for (Activity c : activities) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        activityRepository.saveAll(activities);
    }

    /**
     * Deletes {@link Activity} if the validations are passed.
     *
     * @param id ID of the {@link Activity}
     * @throws DomainEntityNotFoundException if {@link Activity} is not found.
     * @throws OperationNotAllowedException if the {@link Activity} is already deleted.
     * @throws OperationNotAllowedException if the {@link Activity} has active or inactive children.
     * @throws OperationNotAllowedException if the {@link Activity} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACTIVITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Activity with ID: {}", id);
        Activity activity = activityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found, ID: " + id));

        if (activity.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (activityRepository.hasActiveConnectionsToSubActivity(id)) {
            log.error("You can't delete the nomenclature because it is connected to sub activity;");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to sub activity;");
        }

        if (activityRepository.hasActiveConnectionsToProductContract(id)) {
            log.error("You can't delete the nomenclature because it is connected to product contract;");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to product contract;");
        }

        if (activityRepository.hasActiveConnectionsToServiceContract(id)) {
            log.error("You can't delete the nomenclature because it is connected to service contract;");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to service contract;");
        }

        if (activityRepository.hasActiveConnectionsToServiceOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to service order;");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to service order;");
        }

        if (activityRepository.hasActiveConnectionsToGoodsOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to goods order;");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to goods order;");
        }

        activity.setStatus(DELETED);

        activityRepository.save(activity);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return activityRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return activityRepository.findByIdIn(ids);
    }

    /**
     * Filters the list of activities based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Activity}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of ActivityResponse objects containing the filtered list of activities.
     */
    public Page<ActivityResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering activities list with request: {}", request.toString());
        Page<Activity> page = activityRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ActivityResponse::new);
    }

    /**
     * Retrieves detailed information about {@link Activity} by ID
     *
     * @param id ID of {@link Activity}
     * @return {@link ActivityResponse}
     * @throws DomainEntityNotFoundException if no {@link Activity} was found with the provided ID.
     */
    public ActivityResponse view(Long id) {
        log.debug("Fetching activity with ID: {}", id);
        Activity activity = activityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found, ID: " + id));
        return new ActivityResponse(activity);
    }

    /**
     * Adds {@link Activity} at the end with the highest ordering ID.
     * If the request asks to save {@link Activity} as a default and a default {@link Activity} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ActivityRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link ActivityResponse}
     */
    @Transactional
    public ActivityResponse add(ActivityRequest request) {
        log.debug("Adding activity: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (activityRepository.countActivityByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Activity with the same name already exists;");
            throw new OperationNotAllowedException("name-Activity with the same name already exists;");
        }

        Long lastSortOrder = activityRepository.findLastOrderingId();
        Activity activity = new Activity(request);
        activity.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Activity> currentDefaultOptional = activityRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                Activity currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                activityRepository.save(currentDefault);
            }
        }
        Activity activityEntity = activityRepository.save(activity);
        return new ActivityResponse(activityEntity);
    }

    /**
     * Edits the {@link Activity}.
     * If the request asks to save {@link Activity} as a default and a default {@link Activity} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link Activity}
     * @param request {@link ActivityRequest}
     * @throws DomainEntityNotFoundException if {@link Activity} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link Activity} is deleted.
     * @return {@link ActivityResponse}
     */
    @Transactional
    public ActivityResponse edit(Long id, ActivityRequest request) {
        log.debug("Editing Activity: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Activity activity = activityRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (activityRepository.countActivityByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !activity.getName().equals(request.getName().trim())) {
            log.error("name-Activity with the same name already exists;");
            throw new OperationNotAllowedException("name-Activity with the same name already exists;");
        }

        if (activity.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !activity.isDefaultSelection()) {
            Optional<Activity> currentDefaultOptional = activityRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                Activity currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                activityRepository.save(currentDefault);
            }
        }
        activity.setDefaultSelection(request.getDefaultSelection());

        activity.setName(request.getName().trim());
        activity.setStatus(request.getStatus());
        return new ActivityResponse(activityRepository.save(activity));
    }
}
