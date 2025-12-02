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
import bg.energo.phoenix.model.request.nomenclature.address.PopulatedPlaceFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.PopulatedPlaceRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.PopulatedPlaceDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.PopulatedPlaceResponse;
import bg.energo.phoenix.model.response.nomenclature.address.tree.PopulatedPlaceTreeResponse;
import bg.energo.phoenix.repository.nomenclature.address.MunicipalityRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopulatedPlaceService implements NomenclatureBaseService {
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final MunicipalityRepository municipalityRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.POPULATED_PLACES;
    }

    /**
     * Filters {@link PopulatedPlace} against the provided {@link PopulatedPlaceFilterRequest}:
     * If municipalityId is provided in {@link PopulatedPlaceFilterRequest}, only those items will be returned which belong to the requested {@link Municipality}.
     * If excludedItemId is provided in {@link PopulatedPlaceFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link PopulatedPlaceFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link PopulatedPlaceFilterRequest}
     * @return {@link Page<PopulatedPlaceResponse> Page&lt;PopulatedPlaceResponse&gt;} containing detailed information
     */
    public Page<PopulatedPlaceResponse> filter(PopulatedPlaceFilterRequest request) {
        log.debug("Fetching populated places list with request: {}", request.toString());
        return populatedPlaceRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getMunicipalityId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Filters {@link PopulatedPlace} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
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
                    @PermissionMapping(context = POPULATED_PLACES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering populated places list with request: {}", request);
        return populatedPlaceRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link PopulatedPlace} at the end with the highest ordering ID.
     * If the request asks to save {@link PopulatedPlace} as a default and a default {@link PopulatedPlace} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link PopulatedPlaceRequest}
     * @return {@link PopulatedPlaceResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link Municipality} provided in the request is not found or is DELETED.
     */
    @Transactional
    public PopulatedPlaceResponse add(PopulatedPlaceRequest request) {
        log.debug("Adding populated place: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }


        if (populatedPlaceRepository.countPopulatedPlacesByStatusMunicipalitiesAndName(request.getName(), request.getMunicipalityId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-PopulatedPlace with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-PopulatedPlace with the same name and parent already exists;");
        }

        Municipality municipality = municipalityRepository
                .findById(request.getMunicipalityId())
                .orElseThrow(() -> new DomainEntityNotFoundException("municipalityId-Municipality not found, ID: " + request.getMunicipalityId()));

        if (municipality.getStatus().equals(DELETED)) {
            log.error("Cannot add populated place to DELETED municipality");
            throw new ClientException("municipalityId-Cannot add populated place to DELETED municipality", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = populatedPlaceRepository.findLastOrderingId();
        PopulatedPlace populatedPlace = new PopulatedPlace(request);
        populatedPlace.setOrderingId(topId == null ? 1 : topId + 1);
        populatedPlace.setMunicipality(municipality);

        if (request.getDefaultSelection()) {
            Optional<PopulatedPlace> currentDefaultPopulatedPlaceOptional = populatedPlaceRepository.findByDefaultSelectionTrue();
            if (currentDefaultPopulatedPlaceOptional.isPresent()) {
                PopulatedPlace currentDefaultPopulatedPlace = currentDefaultPopulatedPlaceOptional.get();
                currentDefaultPopulatedPlace.setDefaultSelection(false);
                populatedPlaceRepository.save(currentDefaultPopulatedPlace);
            }
        }

        return new PopulatedPlaceResponse(populatedPlaceRepository.save(populatedPlace));
    }

    /**
     * Retrieves detailed information about {@link PopulatedPlace} by ID
     *
     * @param id ID of {@link PopulatedPlace}
     * @throws DomainEntityNotFoundException if no {@link PopulatedPlace} was found with the provided ID.
     * @return {@link PopulatedPlaceResponse}
     */
    public PopulatedPlaceResponse view(Long id) {
        log.debug("Fetching populated place with ID: {}", id);
        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found, ID: " + id));
        return new PopulatedPlaceResponse(populatedPlace);
    }

    /**
     * Retrieves extended information about {@link PopulatedPlace} including information about its parent {@link Municipality}
     *
     * @param id ID of {@link PopulatedPlace}
     * @throws DomainEntityNotFoundException if no {@link PopulatedPlace} was found with the provided ID.
     * @return {@link PopulatedPlaceDetailedResponse}
     */
    public PopulatedPlaceDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed populated place with ID: {}", id);
        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found, ID: " + id));
        return new PopulatedPlaceDetailedResponse(populatedPlace);
    }

    /**
     * Edits the requested {@link PopulatedPlace}.
     * If the request asks to save {@link PopulatedPlace} as a default and a default {@link PopulatedPlace} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link PopulatedPlace}
     * @param request {@link PopulatedPlaceRequest}
     * @throws DomainEntityNotFoundException if {@link PopulatedPlace} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link PopulatedPlace} is deleted.
     * @throws DomainEntityNotFoundException if the {@link Municipality} provided in the request is not found.
     * @throws IllegalArgumentException if the {@link Municipality} provided in the request has DELETED status.
     * @return {@link PopulatedPlaceResponse}
     */
    @Transactional
    public PopulatedPlaceResponse edit(Long id, PopulatedPlaceRequest request) {
        log.debug("Editing municipality: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found, ID: " + id));

        if (populatedPlaceRepository.countPopulatedPlacesByStatusMunicipalitiesAndName(request.getName(), request.getMunicipalityId(), List.of(ACTIVE, INACTIVE), populatedPlace.getId()) > 0) {
            log.error("name-PopulatedPlace with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-PopulatedPlace with the same name and parent already exists;");
        }

        if (populatedPlace.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!populatedPlace.getMunicipality().getId().equals(request.getMunicipalityId())) {
            Municipality municipality = municipalityRepository
                    .findById(request.getMunicipalityId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("municipalityId-Municipality not found, ID: " + request.getMunicipalityId()));

            if (municipality.getStatus().equals(DELETED)) {
                log.error("Cannot add populated place to DELETED municipality");
                throw new ClientException("municipalityId-Cannot add populated place to DELETED municipality", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            populatedPlace.setMunicipality(municipality);
        }

        if (request.getDefaultSelection() && !populatedPlace.isDefaultSelection()) {
            Optional<PopulatedPlace> currentDefaultPopulatedPlaceOptional = populatedPlaceRepository.findByDefaultSelectionTrue();
            if (currentDefaultPopulatedPlaceOptional.isPresent()) {
                PopulatedPlace currentDefaultPopulatedPlace = currentDefaultPopulatedPlaceOptional.get();
                currentDefaultPopulatedPlace.setDefaultSelection(false);
                populatedPlaceRepository.save(currentDefaultPopulatedPlace);
            }
        }
        populatedPlace.setDefaultSelection(request.getDefaultSelection());

        populatedPlace.setName(request.getName().trim());
        populatedPlace.setStatus(request.getStatus());
        return new PopulatedPlaceResponse(populatedPlaceRepository.save(populatedPlace));
    }

    /**
     * Deletes {@link PopulatedPlace} if the validations are passed.
     *
     * @param id ID of the {@link PopulatedPlace}
     * @throws DomainEntityNotFoundException if {@link PopulatedPlace} is not found.
     * @throws OperationNotAllowedException if the {@link PopulatedPlace} is already deleted.
     * @throws OperationNotAllowedException if the {@link PopulatedPlace} has active or inactive children.
     * @throws OperationNotAllowedException if the {@link PopulatedPlace} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POPULATED_PLACES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing populated place with ID: {}", id);
        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found, ID: " + id));

        if (populatedPlace.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (hasActiveOrInactiveChildren(populatedPlace)){
            log.error("Item has active or inactive children.");
            throw new OperationNotAllowedException("id-Item has active or inactive children.");
        }

        if (populatedPlaceRepository.getActiveConnectionsCount(id) > 0) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        populatedPlace.setStatus(DELETED);
        populatedPlaceRepository.save(populatedPlace);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return populatedPlaceRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return populatedPlaceRepository.findByIdIn(ids);
    }

    /**
     * @param populatedPlace ID of {@link PopulatedPlace}
     * @return true if {@link PopulatedPlace} has active or inactive children.
     */
    private boolean hasActiveOrInactiveChildren(PopulatedPlace populatedPlace) {
        Long childDistrictsCount = populatedPlaceRepository
                .getDistrictsCountByStatusAndPopulatedPlaceId(List.of(ACTIVE, INACTIVE), populatedPlace.getId());
        if (childDistrictsCount > 0) {
            return true;
        }

        Long childStreetsCount = populatedPlaceRepository
                .getStreetsCountByStatusAndPopulatedPlaceId(List.of(ACTIVE, INACTIVE), populatedPlace.getId());
        if (childStreetsCount > 0) {
            return true;
        }

        Long childResidentialAreasCount = populatedPlaceRepository
                .getResidentialAreasCountByStatusAndPopulatedPlaceId(List.of(ACTIVE, INACTIVE), populatedPlace.getId());
        if (childResidentialAreasCount > 0) {
            return true;
        }

        Long childZipCodesCount = populatedPlaceRepository
                .getZipCodesCountByStatusAndPopulatedPlaceId(List.of(ACTIVE, INACTIVE), populatedPlace.getId());
        return childZipCodesCount > 0;
    }

    /**
     * Sorts all {@link PopulatedPlace} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POPULATED_PLACES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the populated places alphabetically");
        List<PopulatedPlace> populatedPlaces = populatedPlaceRepository.orderByName();
        long orderingId = 1;

        for (PopulatedPlace p : populatedPlaces) {
            p.setOrderingId(orderingId);
            orderingId++;
        }

        populatedPlaceRepository.saveAll(populatedPlaces);
    }

    /**
     * Changes the ordering of a {@link PopulatedPlace} item in the populated places list to a specified position.
     * The method retrieves the {@link PopulatedPlace} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the populated place item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link PopulatedPlace} item with the given ID is found
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POPULATED_PLACES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of populated place item with ID: {} to place {}", request.getId(), request.getOrderingId());

        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<PopulatedPlace> populatedPlaces;

        if (populatedPlace.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = populatedPlace.getOrderingId();
            populatedPlaces = populatedPlaceRepository.findInOrderingIdRange(
                    start,
                    end,
                    populatedPlace.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (PopulatedPlace p : populatedPlaces) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = populatedPlace.getOrderingId();
            end = request.getOrderingId();
            populatedPlaces = populatedPlaceRepository.findInOrderingIdRange(
                    start,
                    end,
                    populatedPlace.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (PopulatedPlace p : populatedPlaces) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        populatedPlace.setOrderingId(request.getOrderingId());
        populatedPlaces.add(populatedPlace);
        populatedPlaceRepository.saveAll(populatedPlaces);
    }

    /**
     * Returns information (ID, name) about the upper chain of the {@link PopulatedPlace}:
     * <ul>
     *     <li>{@link Municipality}</li>
     *     <li>{@link Region}</li>
     *     <li>{@link Country}</li>
     * </ul>
     *
     * @param id ID of {@link District}
     * @return {@link PopulatedPlaceTreeResponse}
     */
    public PopulatedPlaceTreeResponse treeView(Long id) {
        log.debug("Querying populated place for tree view, ID: {}", id);
        if (!populatedPlaceRepository.existsById(id)) {
            log.debug("Populated place not found, ID " + id);
            throw new DomainEntityNotFoundException("id-Populated place not found, ID " + id);
        }
        return populatedPlaceRepository.getPopulatedPlaceTreeView(id);
    }
}
