package bg.energo.phoenix.service.nomenclature.address;


import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.address.*;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.StreetsFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.StreetsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.StreetsDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.StreetsResponse;
import bg.energo.phoenix.model.response.nomenclature.address.tree.StreetTreeResponse;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.StreetRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.STREETS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
@Service
@RequiredArgsConstructor
@Slf4j
public class StreetService implements NomenclatureBaseService {

    private final StreetRepository repository;
    private final PopulatedPlaceRepository populatedPlaceRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.STREETS;
    }

    /**
     * Filters {@link Street} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link Street}'s name</li>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = STREETS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering streets nomenclature with request: {}", request.toString());
        return repository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Filters {@link Street} against the provided {@link StreetsFilterRequest}:
     * If populatedPlaceId is provided in {@link StreetsFilterRequest}, only those items will be returned which belong to the requested {@link PopulatedPlace}.
     * If excludedItemId is provided in {@link StreetsFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link StreetsFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link Street}'s name</li>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link StreetsFilterRequest}
     * @return {@link Page<StreetsResponse> Page&lt;StreetsResponse&gt;} containing detailed information
     */
    public Page<StreetsResponse> filter(StreetsFilterRequest request){
        log.debug("Filtering streets nomenclature with request: {}", request.toString());
        return repository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getPopulatedPlaceId(),
                        request.getStreetType(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(StreetsResponse::new);
    }

    /**
     * Changes the ordering of a {@link Street} item in the streets list to a specified position.
     * The method retrieves the {@link Street} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Street} item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link Street} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = STREETS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of streets with request: {}", request.toString());
        Street street = repository
                .findByIdAndStatuses(request.getId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new ClientException("id-Street does not exist",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<Street> streets;

        if (street.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = street.getOrderingId();

            streets = repository.findInOrderingIdRange(
                    start,
                    end,
                    street.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Street c : streets) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = street.getOrderingId();
            end = request.getOrderingId();

            streets = repository.findInOrderingIdRange(
                    start,
                    end,
                    street.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Street c : streets) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        street.setOrderingId(request.getOrderingId());
        streets.add(street);
        repository.saveAll(streets);
    }

    /**
     * Sorts all {@link Street} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = STREETS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting streets alphabetically");
        List<Street> streets = repository.orderByName();
        long tempOrderId = 1;

        for (Street p : streets) {
            p.setOrderingId(tempOrderId);
            tempOrderId++;
        }

        repository.saveAll(streets);
    }

    /**
     * Deletes {@link Street} if the validations are passed.
     * @param id ID of the {@link Street}
     * @throws DomainEntityNotFoundException if {@link Street} is not found.
     * @throws OperationNotAllowedException if the {@link Street} is already deleted.
     * @throws OperationNotAllowedException if the {@link Street} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = STREETS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing street with id: {}", id);
        Street street = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Street does not exist",DOMAIN_ENTITY_NOT_FOUND));

        if (street.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (repository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        street.setStatus(DELETED);
        repository.save(street);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return repository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return repository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link Street} by ID
     *
     * @param id ID of {@link Street}
     * @throws DomainEntityNotFoundException if no {@link Street} was found with the provided ID.
     * @return {@link StreetsResponse}
     */
    public StreetsResponse view(Long id) {
        log.debug("Viewing street with id: {}", id);
        Street street = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Street does not exist",DOMAIN_ENTITY_NOT_FOUND));
        return new StreetsResponse(street);
    }

    /**
     * Retrieves extended information about {@link Street} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link Street}
     * @throws DomainEntityNotFoundException if no {@link Street} was found with the provided ID.
     * @return {@link StreetsDetailedResponse}
     */
    public StreetsDetailedResponse detailedView(Long id) {
        log.debug("Viewing detailed street with id: {}", id);
        Street street = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Street does not exist",DOMAIN_ENTITY_NOT_FOUND));
        return new StreetsDetailedResponse(street);
    }

    /**
     * Adds {@link Street} at the end with the highest ordering ID.
     * If the request asks to save {@link Street} as a default and a default {@link Street} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link StreetsRequest}
     * @return {@link StreetsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link PopulatedPlace} provided in the request is not found or is DELETED.
     */
    @Transactional
    public StreetsResponse add(StreetsRequest request) {
        log.debug("Adding street with request: {}", request.toString());
        if (request.getStatus().equals(DELETED)) {
            throw new ClientException("status-Cannot add street with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (repository.countStreetByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-Street/Boulevard with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Street/Boulevard with the same name and parent already exists;");
        }

        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(request.getPopulatedPlaceId())
                .orElseThrow(() -> new ClientException("populatedPlaceId-Populated place with id: " +
                        request.getPopulatedPlaceId() + " does not exist", DOMAIN_ENTITY_NOT_FOUND));
        if (populatedPlace.getStatus().equals(DELETED)) {
            throw new ClientException("populatedPlaceId-Cannot add street to deleted populated place", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Street street = new Street(request);
        street.setPopulatedPlace(populatedPlace);
        Long lastOrderingId = repository.findLastOrderingId();
        street.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        if (request.getDefaultSelection()) {
            Optional<Street> currDefault = repository.findByDefaultSelectionTrue();
            if (currDefault.isPresent()) {
                Street def = currDefault.get();
                def.setDefaultSelection(false);
                repository.save(def);
            }
        }
        Street save = repository.save(street);
        return new StreetsResponse(save);
    }

    /**
     * Retrieves extended information about {@link Street} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link Street}
     * @throws DomainEntityNotFoundException if no {@link Street} was found with the provided ID.
     * @return {@link StreetsDetailedResponse}
     */
    @Transactional
    public StreetsResponse edit(Long id, StreetsRequest request) {
        log.debug("Editing street with id: {} and request: {}", id, request.toString());
        if (request.getStatus().equals(DELETED)) {
            throw new ClientException("status-Cannot edit street with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Street street = repository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (repository.countStreetByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), street.getId()) > 0) {
            log.error("name-Street/Boulevard with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Street/Boulevard with the same name and parent already exists;");
        }

        if (street.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!street.getPopulatedPlace().getId().equals(request.getPopulatedPlaceId())) {
            if(checkStreetIsAssignedToActiveRecord(street.getId())){
                log.error("Street is assigned to active record, Updating Populated place is not allowed");
                throw new OperationNotAllowedException("Street is assigned to active record, Updating Populated place is not allowed");
            }

            PopulatedPlace populatedPlace = populatedPlaceRepository
                    .findById(request.getPopulatedPlaceId())
                    .orElseThrow(() -> new ClientException("populatedPlaceId-Populated place with id: " +
                            request.getPopulatedPlaceId() + " does not exist", DOMAIN_ENTITY_NOT_FOUND));
            if (populatedPlace.getStatus().equals(DELETED)) {
                throw new ClientException("populatedPlaceId-Cannot add street to deleted populated place", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            street.setPopulatedPlace(populatedPlace);
        }

        if (request.getDefaultSelection() && !street.getDefaultSelection()) {
            Optional<Street> streetDef = repository.findByDefaultSelectionTrue();
            if (streetDef.isPresent()) {
                Street def = streetDef.get();
                def.setDefaultSelection(false);
                repository.save(def);
            }
        }
        street.setDefaultSelection(request.getDefaultSelection());

        street.setName(request.getName().trim());
        street.setStatus(request.getStatus());
        street.setType(request.getType());
        return new StreetsResponse(repository.save(street));
    }

    private boolean checkStreetIsAssignedToActiveRecord(Long id) {
            return repository.getActiveConnectionsCount(id) > 0;
    }

    /**
     * Returns information (ID, name) about the upper chain of the {@link Street}:
     * <ul>
     *     <li>{@link PopulatedPlace}</li>
     *     <li>{@link Municipality}</li>
     *     <li>{@link Region}</li>
     *     <li>{@link Country}</li>
     * </ul>
     *
     * @param id ID of {@link Street}
     * @return {@link StreetTreeResponse}
     */
    public StreetTreeResponse treeView(Long id) {
        log.debug("Querying street for tree view, ID: {}", id);
        if (!repository.existsById(id)) {
            throw new DomainEntityNotFoundException("id-street not found, ID " + id);
        }
        return repository.getStreetTreeView(id);
    }

}
