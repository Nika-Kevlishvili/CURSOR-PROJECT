package bg.energo.phoenix.service.nomenclature.address;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.address.*;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.ResidentialAreaFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.ResidentialAreaRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.ResidentialAreaDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.ResidentialAreaResponse;
import bg.energo.phoenix.model.response.nomenclature.address.tree.ResidentialAreaTreeResponse;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.ResidentialAreaRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.RESIDENTIAL_AREAS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResidentialAreaService implements NomenclatureBaseService {

    private final ResidentialAreaRepository repository;
    private final PopulatedPlaceRepository populatedPlaceRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.RESIDENTIAL_AREAS;
    }

    /**
     * Filters {@link ResidentialArea} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link ResidentialArea}'s name</li>
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
                    @PermissionMapping(context = RESIDENTIAL_AREAS, permissions = {NOMENCLATURE_VIEW}),
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
                        PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Filters {@link ResidentialArea} against the provided {@link ResidentialAreaFilterRequest}:
     * If populatedPlaceId is provided in {@link ResidentialAreaFilterRequest}, only those items will be returned which belong to the requested {@link PopulatedPlace}.
     * If excludedItemId is provided in {@link ResidentialAreaFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link ResidentialAreaFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link ResidentialArea}'s name</li>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link ResidentialAreaFilterRequest}
     * @return {@link Page<ResidentialAreaResponse> Page&lt;ResidentialAreaResponse&gt;} containing detailed information
     */
    public Page<ResidentialAreaResponse> filter(ResidentialAreaFilterRequest request) {
        log.debug("Filtering streets nomenclature with request: {}", request.toString());
        return repository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getPopulatedPlaceId(),
                        request.getResidentialAreaType(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(ResidentialAreaResponse::new);

    }

    /**
     * Changes the ordering of a {@link ResidentialArea} item in the residential areas list to a specified position.
     * The method retrieves the {@link ResidentialArea} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ResidentialArea} item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link ResidentialArea} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RESIDENTIAL_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing residential areas order with request: {}", request.toString());

        ResidentialArea area = repository
                .findByIdAndStatuses(request.getId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new ClientException("id-Residential are does not exist",ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ResidentialArea> residentialAreas;

        if (area.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = area.getOrderingId();

            residentialAreas = repository.findInOrderingIdRange(
                    start,
                    end,
                    area.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ResidentialArea r : residentialAreas) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = area.getOrderingId();
            end = request.getOrderingId();

            residentialAreas = repository.findInOrderingIdRange(
                    start,
                    end,
                    area.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ResidentialArea r : residentialAreas) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        area.setOrderingId(request.getOrderingId());
        residentialAreas.add(area);
        repository.saveAll(residentialAreas);
    }

    /**
     * Sorts all {@link ResidentialArea} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RESIDENTIAL_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting residential areas alphabetically");
        List<ResidentialArea> residentialAreas = repository.orderByName();
        long tempOrderingId = 1;
        for (ResidentialArea r : residentialAreas) {
            r.setOrderingId(tempOrderingId);
            tempOrderingId += 1;
        }
        repository.saveAll(residentialAreas);
    }

    /**
     * Deletes {@link ResidentialArea} if the validations are passed.
     * @param id ID of the {@link ResidentialArea}
     * @throws DomainEntityNotFoundException if {@link ResidentialArea} is not found.
     * @throws OperationNotAllowedException if the {@link ResidentialArea} is already deleted.
     * @throws OperationNotAllowedException if the {@link ResidentialArea} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RESIDENTIAL_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing residential area with id: {}", id);
        ResidentialArea residentialArea = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Residential area not found: id = " + id, ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (residentialArea.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (repository.getActiveConnectionsCount(id) > 0) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        residentialArea.setStatus(DELETED);
        repository.save(residentialArea);
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
     * Adds {@link ResidentialArea} at the end with the highest ordering ID.
     * If the request asks to save {@link ResidentialArea} as a default and a default {@link ResidentialArea} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ResidentialAreaRequest}
     * @return {@link ResidentialAreaResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link PopulatedPlace} provided in the request is not found or is DELETED.
     */
    @Transactional
    public ResidentialAreaResponse add(ResidentialAreaRequest request) {
        log.debug("Adding residential area with request: {}", request.toString());
        PopulatedPlace place = populatedPlaceRepository.findById(request.getPopulatedPlaceId())
                .orElseThrow(() -> new ClientException("populatedPlaceId-Populated place not found: id = " +
                        request.getPopulatedPlaceId(), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        if (place.getStatus().equals(DELETED)) {
            throw new ClientException("populatedPlaceId-Populated place not found: id =  " +
                    request.getPopulatedPlaceId(), ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        if (repository.countResidentialAreaByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-Quarter/Residential area with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Quarter/Residential area with the same name and parent already exists;");
        }

        ResidentialArea residentialArea = new ResidentialArea(request);
        residentialArea.setPopulatedPlace(place);
        Long lastSortOrder = repository.findLastOrderingId();
        residentialArea.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<ResidentialArea> defaultArea = repository.findByDefaultSelectionTrue();
            if (defaultArea.isPresent()) {
                ResidentialArea def = defaultArea.get();
                def.setDefaultSelection(false);
                repository.save(def);
            }
        }
        ResidentialArea save = repository.save(residentialArea);
        return new ResidentialAreaResponse(save);
    }

    /**
     * Retrieves extended information about {@link ResidentialArea} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link ResidentialArea}
     * @throws DomainEntityNotFoundException if no {@link ResidentialArea} was found with the provided ID.
     * @return {@link ResidentialAreaDetailedResponse}
     */
    @Transactional
    public ResidentialAreaResponse edit(Long id, ResidentialAreaRequest request) {
        log.debug("Editing residential area with request: {}", request.toString());
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot edit residential area with status DELETED");
            throw new ClientException
                    ("status-Cannot edit residential area with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ResidentialArea residentialArea = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("populatedPlaceId-Residential area not found: id = " + id, ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (repository.countResidentialAreaByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), residentialArea.getId()) > 0) {
            log.error("name-Quarter/Residential area with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Quarter/Residential area with the same name and parent already exists;");
        }


        if (residentialArea.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new ClientException("populatedPlaceId-Populated place not found: id =  " +
                    request.getPopulatedPlaceId(), ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        if (!residentialArea.getPopulatedPlace().getId().equals(request.getPopulatedPlaceId())) {
            if (checkResidentialAreaIsAssignedToActiveRecord(residentialArea.getId())) {
                log.error("Residential area is assigned to active record, Updating Populated place is not allowed");
                throw new OperationNotAllowedException("Residential area is assigned to active record, Updating Populated place is not allowed");
            }
            PopulatedPlace populatedPlace = populatedPlaceRepository
                    .findById(request.getPopulatedPlaceId())
                    .orElseThrow(() -> new ClientException("populatedPlaceId-Populated place with id: " +
                            request.getPopulatedPlaceId() + " does not exist", DOMAIN_ENTITY_NOT_FOUND));
            if (populatedPlace.getStatus().equals(DELETED)) {
                log.error("Cannot add street to deleted populated place");
                throw new ClientException("status-Cannot add street to deleted populated place", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            residentialArea.setPopulatedPlace(populatedPlace);
        }

        if (request.getDefaultSelection() && !residentialArea.getDefaultSelection()) {
            Optional<ResidentialArea> defaultArea = repository.findByDefaultSelectionTrue();
            if (defaultArea.isPresent()) {
                ResidentialArea def = defaultArea.get();
                def.setDefaultSelection(false);
                repository.save(def);
            }
        }
        residentialArea.setDefaultSelection(request.getDefaultSelection());

        residentialArea.setName(request.getName().trim());
        residentialArea.setType(request.getType());
        residentialArea.setStatus(request.getStatus());
        ResidentialArea save = repository.save(residentialArea);
        return new ResidentialAreaResponse(save);
    }

    private boolean checkResidentialAreaIsAssignedToActiveRecord(Long id) {
        return repository.getActiveConnectionsCount(id) > 0;
    }

    /**
     * Retrieves detailed information about {@link ResidentialArea} by ID
     *
     * @param id ID of {@link ResidentialArea}
     * @throws DomainEntityNotFoundException if no {@link ResidentialArea} was found with the provided ID.
     * @return {@link ResidentialAreaResponse}
     */
    public ResidentialAreaResponse view(Long id) {
        log.debug("Viewing residential area with id: {}", id);
        ResidentialArea residentialArea = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Residential area not found: id = " + id, ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new ResidentialAreaResponse(residentialArea);
    }

    /**
     * Retrieves extended information about {@link ResidentialArea} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link ResidentialArea}
     * @throws DomainEntityNotFoundException if no {@link ResidentialArea} was found with the provided ID.
     * @return {@link ResidentialAreaDetailedResponse}
     */
    public ResidentialAreaDetailedResponse detailedView(Long id) {
        log.debug("Viewing detailed residential area with id: {}", id);
        ResidentialArea residentialArea = repository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Residential area not found: id = " + id, ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new ResidentialAreaDetailedResponse(residentialArea);
    }

    /**
     * Returns information (ID, name) about the upper chain of the {@link ResidentialArea}:
     * <ul>
     *     <li>{@link PopulatedPlace}</li>
     *     <li>{@link Municipality}</li>
     *     <li>{@link Region}</li>
     *     <li>{@link Country}</li>
     * </ul>
     *
     * @param id ID of {@link ResidentialArea}
     * @return {@link ResidentialAreaTreeResponse}
     */
    public ResidentialAreaTreeResponse treeView(Long id) {
        log.debug("Querying residential areas for tree view, ID: {}", id);
        if (!repository.existsById(id)) {
            throw new DomainEntityNotFoundException("id-Residential area not found, ID " + id);
        }
        return repository.getResidentialAreaTreeView(id);
    }

}
