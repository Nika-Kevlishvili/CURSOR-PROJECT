package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivity;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.SubActivityFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.SubActivityRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.SubActivityResponse;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.SubActivityRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.SUB_ACTIVITIES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubActivityService implements NomenclatureBaseService {
    private final SubActivityRepository subActivityRepository;

    private final ActivityRepository activityRepository;


    /**
     * Filters {@link SubActivity} against the provided {@link SubActivityFilterRequest}:
     * If activityId is provided in {@link SubActivityFilterRequest}, only those items will be returned which belong to the requested {@link Activity}.
     * If excludedItemId is provided in {@link SubActivityFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link SubActivityFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SubActivity}'s name.</li>
     *     <li>{@link Activity}'s name</li>
     * </ul>
     *
     * @param request {@link SubActivityFilterRequest}
     * @return {@link Page<SubActivityResponse> Page&lt;SubActivityResponse&gt;} containing detailed information
     */
    public Page<SubActivityResponse> filter(SubActivityFilterRequest request) {
        log.debug("Fetching subActivities list with request: {}", request.toString());
        Page<SubActivity> page = subActivityRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getActivityId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(SubActivityResponse::new);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SUB_ACTIVITIES;
    }

    /**
     * Filters {@link SubActivity} against the provided {@link NomenclatureItemsBaseFilterRequest}:
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SubActivity}'s name.</li>
     *     <li>{@link Activity}'s name</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<NomenclatureResponse> Page&lt;NomenclatureResponse&gt;} containing detailed information
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SUB_ACTIVITIES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {

        log.debug("Filtering subActivities list with statuses: {}", request);
        return subActivityRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link SubActivity} item in the subactivities list to a specified position.
     * The method retrieves the {@link SubActivity} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link SubActivity} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link SubActivity} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SUB_ACTIVITIES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of subActivity item with ID: {} to place {}", request.getId(), request.getOrderingId());

        SubActivity subActivity = subActivityRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-subActivity not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<SubActivity> subActivities;

        if (subActivity.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = subActivity.getOrderingId();
            subActivities = subActivityRepository.findInOrderingIdRange(
                    start,
                    end,
                    subActivity.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (SubActivity r : subActivities) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = subActivity.getOrderingId();
            end = request.getOrderingId();
            subActivities = subActivityRepository.findInOrderingIdRange(
                    start,
                    end,
                    subActivity.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (SubActivity r : subActivities) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        subActivity.setOrderingId(request.getOrderingId());
        subActivities.add(subActivity);
        subActivityRepository.saveAll(subActivities);
    }

    /**
     * Sorts all {@link SubActivity} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SUB_ACTIVITIES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the subactivity alphabetically");
        List<SubActivity> subActivities = subActivityRepository.orderByName();
        long orderingId = 1;

        for (SubActivity r : subActivities) {
            r.setOrderingId(orderingId);
            orderingId++;
        }

        subActivityRepository.saveAll(subActivities);
    }

    /**
     * Deletes {@link SubActivity} if the validations are passed.
     *
     * @param id ID of the {@link SubActivity}
     * @throws DomainEntityNotFoundException if {@link SubActivity} is not found.
     * @throws OperationNotAllowedException  if the {@link SubActivity} is already deleted.
     * @throws OperationNotAllowedException  if the {@link SubActivity} has active or inactive children.
     * @throws OperationNotAllowedException  if the {@link SubActivity} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SUB_ACTIVITIES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public void delete(Long id) {
        log.debug("Removing subActivity with ID: {}", id);
        SubActivity subActivity = subActivityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-subActivity not found, ID: " + id));

        if (subActivity.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (subActivityRepository.hasActiveConnectionsWithProductContract(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (subActivityRepository.hasActiveConnectionsWithServiceContracts(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (subActivityRepository.hasActiveConnectionsWithServiceOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (subActivityRepository.hasActiveConnectionsWithGoodsOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        subActivity.setStatus(DELETED);

        subActivityRepository.save(subActivity);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return subActivityRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return subActivityRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link SubActivity} at the end with the highest ordering ID.
     * If the request asks to save {@link SubActivity} as a default and a default {@link SubActivity} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link SubActivityRequest}
     * @return {@link SubActivityResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link Activity} provided in the request is not found or is DELETED.
     */

    @Transactional
    public SubActivityResponse add(SubActivityRequest request) {
        log.debug("Adding SubActivity: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (subActivityRepository.countSubActivityByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-SubActivity with the same name already exists;");
            throw new OperationNotAllowedException("name-SubActivity with the same name already exists;");
        }

        Activity activity = activityRepository
                .findById(request.getActivityId())
                .orElseThrow(() -> new DomainEntityNotFoundException("activityId-Activity not found: " + request.getActivityId()));

        if (activity.getStatus().equals(DELETED)) {
            log.error("Cannot add subActivity to DELETED activity");
            throw new ClientException("status-Cannot add subActivity to DELETED activity", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = subActivityRepository.findLastOrderingId();
        SubActivity subActivity = new SubActivity(request);
        subActivity.setOrderingId(topId == null ? 1 : topId + 1);
        subActivity.setActivity(activity);

        if (request.getDefaultSelection()) {
            Optional<SubActivity> currentDefaultSubActivityOptional = subActivityRepository.findByDefaultSelectionTrue();
            if (currentDefaultSubActivityOptional.isPresent()) {
                SubActivity currentDefaultSubActivity = currentDefaultSubActivityOptional.get();
                currentDefaultSubActivity.setDefaultSelection(false);
                subActivityRepository.save(currentDefaultSubActivity);
            }
        }
        return new SubActivityResponse(subActivityRepository.save(subActivity));
    }

    /**
     * Retrieves extended information about {@link SubActivity} including information about its parent {@link Activity}
     *
     * @param id ID of {@link SubActivity}
     * @return {@link SubActivityResponse}
     * @throws DomainEntityNotFoundException if no {@link SubActivity} was found with the provided ID.
     */
    public SubActivityResponse view(Long id) {
        log.debug("Fetching SubActivity with ID: {}", id);
        SubActivity subActivity = subActivityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-SubActivity not found, ID: " + id));
        return new SubActivityResponse(subActivity);
    }

    /**
     * Edits the requested {@link SubActivity}.
     * If the request asks to save {@link SubActivity} as a default and a default {@link SubActivity} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link SubActivity}
     * @param request {@link SubActivityRequest}
     * @return {@link SubActivityResponse}
     * @throws DomainEntityNotFoundException if {@link SubActivity} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link SubActivity} is deleted.
     * @throws DomainEntityNotFoundException if the {@link Activity} provided in the request is not found.
     * @throws IllegalArgumentException      if the {@link Activity} provided in the request has DELETED status.
     */
    @Transactional
    public SubActivityResponse edit(Long id, SubActivityRequest request) {
        log.debug("Editing SubActivity: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        SubActivity subActivity = subActivityRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (subActivityRepository.countSubActivityByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !subActivity.getName().equals(request.getName().trim())) {
            log.error("name-SubActivity with the same name already exists;");
            throw new OperationNotAllowedException("name-SubActivity with the same name already exists;");
        }

        if (subActivity.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!subActivity.getActivity().getId().equals(request.getActivityId())) {
            Activity activity = activityRepository
                    .findById(request.getActivityId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("activityId-Activity not found: " + request.getActivityId()));

            if (activity.getStatus().equals(DELETED)) {
                log.error("Cannot add subActivity to DELETED activity");
                throw new ClientException("activityId-Cannot add subActivity to DELETED activity", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            subActivity.setActivity(activity);
        }

        if (request.getDefaultSelection() && !subActivity.isDefaultSelection()) {
            Optional<SubActivity> currentDefaultSubActivityOptional = subActivityRepository.findByDefaultSelectionTrue();
            if (currentDefaultSubActivityOptional.isPresent()) {
                SubActivity currentDefaultSubActivity = currentDefaultSubActivityOptional.get();
                currentDefaultSubActivity.setDefaultSelection(false);
                subActivityRepository.save(currentDefaultSubActivity);
            }
        }
        subActivity.setDefaultSelection(request.getDefaultSelection());
        subActivity.setFields(request.getFields());
        subActivity.setName(request.getName().trim());
        subActivity.setStatus(request.getStatus());
        return new SubActivityResponse(subActivityRepository.save(subActivity));
    }
}
