package bg.energo.phoenix.service.nomenclature.pod.userType;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.pod.UserType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.UserTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.UserTypeResponse;
import bg.energo.phoenix.repository.nomenclature.pod.UserTypeRepository;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.POD;
import static bg.energo.phoenix.permissions.PermissionContextEnum.USER_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTypeService implements NomenclatureBaseService {

    private final UserTypeRepository userTypeRepository;
    private final UserTypeMapper userTypeMapper;


    /**
     * @return {@link Nomenclature#USER_TYPES}
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.USER_TYPES;
    }


    /**
     * Filters the entities by the given request criteria.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching entity will be excluded from the result.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the search will be performed in {@link UserType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest} containing the filter criteria
     * @return {@link Page<UserTypeResponse>} containing the filtered entities
     */
    public Page<UserTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering user types by request: {}", request);
        Page<UserType> page = userTypeRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(UserTypeResponse::new);
    }


    /**
     * Filters items against the provided {@link NomenclatureItemsBaseFilterRequest} criteria.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item will be excluded from the result.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the search will be performed in {@link UserType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest} containing the filter criteria
     * @return {@link Page<NomenclatureResponse>} containing the filtered items
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = USER_TYPES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(
                            context = POD, permissions = {
                            POD_CREATE,
                            POD_EDIT,
                            POD_EDIT_LOCKED,
                            POD_VIEW_BASIC,
                            POD_VIEW_DELETED
                    }
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering user types by request: {}", request);
        Page<UserType> userTypes = userTypeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return userTypes.map(userTypeMapper::nomenclatureResponseFromEntity);
    }


    /**
     * Adds a new {@link UserType} entity at the end with the highest ordering ID.
     * If the request asks to save {@link UserType} as a default and a default {@link UserType} already exists,
     * the existing default {@link UserType} will be set to non-default and the new {@link UserType} will be set as default.
     *
     * @param request {@link UserTypeRequest} containing the data to be saved
     * @return {@link UserTypeResponse} containing the saved entity
     */
    @Transactional
    public UserTypeResponse add(UserTypeRequest request) {
        log.debug("Adding user type with request: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (userTypeRepository.existsByName(request.getName(), List.of(ACTIVE, INACTIVE))) {
            log.error("name-User type with name {} already exists", request.getName());
            throw new ClientException("name-User type with name [%s] already exists;".formatted(request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = userTypeRepository.findLastOrderingId();
        UserType userType = userTypeMapper.entityFromRequest(request);
        userType.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, userType);

        userTypeRepository.saveAndFlush(userType);
        return userTypeMapper.responseFromEntity(userType);
    }


    /**
     * Sets the default selection flag for the given {@link UserType} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link UserType} is set to false,
     * and the given {@link UserType} is set as the new default selection.
     *
     * @param request  the {@link UserTypeRequest} containing the status and default selection flag to use when setting the default selection
     * @param userType the {@link UserType} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(UserTypeRequest request, UserType userType) {
        if (request.getStatus().equals(INACTIVE)) {
            userType.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<UserType> currentDefaultEntityOptional = userTypeRepository.findByDefaultSelectionTrue();
                if (currentDefaultEntityOptional.isPresent()) {
                    UserType currentDefaultItem = currentDefaultEntityOptional.get();
                    currentDefaultItem.setDefaultSelection(false);
                    userTypeRepository.save(currentDefaultItem);
                }
            }
            userType.setDefaultSelection(request.getDefaultSelection());
        }
    }


    /**
     * Changes the ordering of a {@link UserType} item in the list to a specified position.
     * The method retrieves the {@link UserType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link UserType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link UserType} item with the given ID is found
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = USER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    @Transactional
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in user types", request.getId());

        UserType userType = userTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-User type not found by ID: %s;".formatted(request.getId())));

        Long start;
        Long end;
        List<UserType> userTypes;

        if (userType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = userType.getOrderingId();
            userTypes = userTypeRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            userType.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (UserType ut : userTypes) {
                ut.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = userType.getOrderingId();
            end = request.getOrderingId();
            userTypes = userTypeRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            userType.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (UserType ut : userTypes) {
                ut.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        userType.setOrderingId(request.getOrderingId());
        userTypes.add(userType);
        userTypeRepository.saveAll(userTypes);
    }


    /**
     * Sorts all entities alphabetically not taking status into consideration.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = USER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    @Transactional
    public void sortAlphabetically() {
        log.debug("Sorting the user types alphabetically");

        List<UserType> userTypes = userTypeRepository.orderByName();
        long orderingId = 1;

        for (UserType ut : userTypes) {
            ut.setOrderingId(orderingId);
            orderingId++;
        }

        userTypeRepository.saveAll(userTypes);
    }


    /**
     * Sets DELETED status to {@link UserType} entity if the validations are passed.
     *
     * @param id ID of the {@link UserType} to be deleted.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = USER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Removing user type with ID: {}", id);

        UserType userType = userTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-User type not found, ID: %s;".formatted(id)));

        if (userType.getStatus().equals(DELETED)) {
            log.error("User type with ID {} is already deleted;", id);
            throw new OperationNotAllowedException("User type with ID [%s] is already deleted;".formatted(id));
        }

        if (userTypeRepository.hasActiveConnections(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        userType.setStatus(DELETED);
        userTypeRepository.save(userType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return userTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return userTypeRepository.findByIdIn(ids);
    }


    /**
     * Retrieves detailed information about {@link UserType} by ID.
     *
     * @param id ID of the {@link UserType} to be fetched.
     * @return {@link UserTypeResponse} object
     */
    public UserTypeResponse view(Long id) {
        log.debug("Fetching user type with ID: {}", id);
        UserType userType = userTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-User type not found with ID: [%s];".formatted(id)));
        return userTypeMapper.responseFromEntity(userType);
    }


    /**
     * Edits {@link UserType} entity if the validations are passed.
     * If the request asks to save {@link UserType} as a default and a default {@link UserType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * If the status inside request is {@link NomenclatureItemStatus#INACTIVE}, default selection will be set false,
     * no matter the requested default selection.
     *
     * @param id      ID of the {@link UserType} to be edited.
     * @param request {@link UserTypeRequest} object containing the new data.
     * @return {@link UserTypeResponse} object containing the updated data.
     */
    @Transactional
    public UserTypeResponse edit(Long id, UserTypeRequest request) {
        log.debug("Editing user type with ID: {}, request: {}", id, request);

        if (request.getStatus().equals(DELETED)) {
            log.error("id-You can't set DELETED statuse to nomenclature item;");
            throw new OperationNotAllowedException("id-You can't set DELETED statuse to nomenclature item;");
        }

        UserType userType = userTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-User type not found with ID: [%s];".formatted(id)));

        if (userType.getStatus().equals(DELETED)) {
            log.error("Can't edit the nomenclature because it is deleted");
            throw new OperationNotAllowedException("id-You can't edit the nomenclature because it is deleted");
        }

        // if the name is changed, check if the new name is unique
        if (userTypeRepository.existsByName(request.getName(), List.of(ACTIVE, INACTIVE)) && !userType.getName().equals(request.getName())) {
            log.error("id-User type with name {} already exists;", request.getName());
            throw new OperationNotAllowedException("id-User type with name [%s] already exists;".formatted(request.getName()));
        }

        assignDefaultSelectionWhenEditing(request, userType);

        userType.setName(request.getName());
        userType.setStatus(request.getStatus());
        userTypeRepository.save(userType);

        return userTypeMapper.responseFromEntity(userType);
    }


    /**
     * Sets the default selection flag for the given {@link UserType} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link UserType} is set to false,
     * and the given {@link UserType} is set as the new default selection.
     *
     * @param request  the {@link UserTypeRequest} containing the status and default selection flag to use when setting the default selection
     * @param userType the {@link UserType} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(UserTypeRequest request, UserType userType) {
        if (request.getStatus().equals(INACTIVE)) {
            userType.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!userType.isDefaultSelection()) {
                    Optional<UserType> currentDefaultEntityOptional = userTypeRepository.findByDefaultSelectionTrue();
                    if (currentDefaultEntityOptional.isPresent()) {
                        UserType currentDefaultEntity = currentDefaultEntityOptional.get();
                        currentDefaultEntity.setDefaultSelection(false);
                        userTypeRepository.save(currentDefaultEntity);
                    }
                }
            }
            userType.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
