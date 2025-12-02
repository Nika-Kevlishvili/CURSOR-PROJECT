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
import bg.energo.phoenix.model.request.nomenclature.address.DistrictFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.DistrictRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.DistrictDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.DistrictResponse;
import bg.energo.phoenix.model.response.nomenclature.address.tree.DistrictTreeResponse;
import bg.energo.phoenix.repository.nomenclature.address.DistrictRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.DISTRICTS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistrictService implements NomenclatureBaseService {
    private final DistrictRepository districtRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.DISTRICTS;
    }

    /**
     * Filters {@link Country} against the provided {@link DistrictFilterRequest}:
     * If populatedPlaceId is provided in {@link DistrictFilterRequest}, only those items will be returned which belong to the requested {@link PopulatedPlace}.
     * If excludedItemId is provided in {@link DistrictFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link DistrictFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link District}'s name</li>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link DistrictFilterRequest}
     * @return {@link Page<DistrictResponse> Page&lt;DistrictResponse&gt;} containing detailed information
     */
    public Page<DistrictResponse> filter(DistrictFilterRequest request) {
        log.debug("Fetching districts list with request: {}", request.toString());
        Page<District> page = districtRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getPopulatedPlaceId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(DistrictResponse::new);
    }

    /**
     * Filters {@link District} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link District}'s name</li>
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
                    @PermissionMapping(context = DISTRICTS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering districts list with request: {}", request);
        return districtRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link District} at the end with the highest ordering ID.
     * If the request asks to save {@link District} as a default and a default {@link District} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link DistrictRequest}
     * @return {@link DistrictResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link PopulatedPlace} provided in the request is not found or is DELETED.
     */
    @Transactional
    public DistrictResponse add(DistrictRequest request) {
        log.debug("Adding district: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (districtRepository.countDistrictByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-District with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-District with the same name and parent already exists;");
        }

        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(request.getPopulatedPlaceId())
                .orElseThrow(() -> new DomainEntityNotFoundException("populatedPlaceId-Populated place not found, ID: " + request.getPopulatedPlaceId()));

        if (populatedPlace.getStatus().equals(DELETED)) {
            log.error("Cannot add district to DELETED populated place");
            throw new ClientException("populatedPlaceId-Cannot add district to DELETED populated place", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = districtRepository.findLastOrderingId();
        District district = new District(request);
        district.setOrderingId(topId == null ? 1 : topId + 1);
        district.setPopulatedPlace(populatedPlace);

        if (request.getDefaultSelection()) {
            Optional<District> currentDefaultDistrictOptional = districtRepository.findByDefaultSelectionTrue();
            if (currentDefaultDistrictOptional.isPresent()) {
                District currentDefaultDistrict = currentDefaultDistrictOptional.get();
                currentDefaultDistrict.setDefaultSelection(false);
                districtRepository.save(currentDefaultDistrict);
            }
        }

        return new DistrictResponse(districtRepository.save(district));
    }

    /**
     * Retrieves detailed information about {@link District} by ID
     *
     * @param id ID of {@link District}
     * @throws DomainEntityNotFoundException if no {@link District} was found with the provided ID.
     * @return {@link DistrictResponse}
     */
    public DistrictResponse view(Long id) {
        log.debug("Fetching district with ID: {}", id);
        District district = districtRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-District not found, ID: " + id));
        return new DistrictResponse(district);
    }

    /**
     * Retrieves extended information about {@link District} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link District}
     * @throws DomainEntityNotFoundException if no {@link District} was found with the provided ID.
     * @return {@link DistrictDetailedResponse}
     */
    public DistrictDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed district with ID: {}", id);
        District district = districtRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-District not found, ID: " + id));
        return new DistrictDetailedResponse(district);
    }

    /**
     * Edit the requested {@link District}.
     * If the request asks to save {@link District} as a default and a default {@link District} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link District}
     * @param request {@link DistrictRequest}
     * @throws DomainEntityNotFoundException if {@link District} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link District} is deleted.
     * @throws OperationNotAllowedException if changing the parent {@link PopulatedPlace} is requested and the {@link District} is already is assigned to active record.
     * @throws DomainEntityNotFoundException if the {@link PopulatedPlace} provided in the request is not found
     * @throws IllegalArgumentException if the {@link PopulatedPlace} provided in the request has DELETED status.
     * @return {@link DistrictResponse}
     */
    @Transactional
    public DistrictResponse edit(Long id, DistrictRequest request) {
        log.debug("Editing district: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        District district = districtRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-District not found, ID: " + id));

        if (districtRepository.countDistrictByStatusPopulatedPlaceAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), district.getId()) > 0) {
            log.error("name-District with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-District with the same name and parent already exists;");
        }

        if (district.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!district.getPopulatedPlace().getId().equals(request.getPopulatedPlaceId())) {
            if (checkDistrictIsAssignedToActiveRecord(district.getId())) {
                log.error("District is assigned to active record, Updating Populated place is not allowed");
                throw new OperationNotAllowedException("populatedPlaceId-District is assigned to active record, Updating Populated place is not allowed");
            }

            PopulatedPlace populatedPlace = populatedPlaceRepository
                    .findById(request.getPopulatedPlaceId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("populatedPlaceId-Populated place not found, ID: " + request.getPopulatedPlaceId()));

            if (populatedPlace.getStatus().equals(DELETED)) {
                log.error("Cannot add district to DELETED populated place");
                throw new ClientException("populatedPlaceId-Cannot add district to DELETED populated place", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            district.setPopulatedPlace(populatedPlace);
        }

        if (request.getDefaultSelection() && !district.isDefaultSelection()) {
            Optional<District> currentDefaultDistrictOptional = districtRepository.findByDefaultSelectionTrue();
            if (currentDefaultDistrictOptional.isPresent()) {
                District currentDefaultDistrict = currentDefaultDistrictOptional.get();
                currentDefaultDistrict.setDefaultSelection(false);
                districtRepository.save(currentDefaultDistrict);
            }
        }
        district.setDefaultSelection(request.getDefaultSelection());

        district.setName(request.getName().trim());
        district.setStatus(request.getStatus());
        return new DistrictResponse(districtRepository.save(district));
    }

    /**
     * Checks if {@link District} is already assigned to active record.
     * @param id ID of the {@link District}
     * @return true if {@link District} has active connections.
     */
    private boolean checkDistrictIsAssignedToActiveRecord(Long id) {
        return districtRepository.getActiveConnectionsCount(id) > 0;
    }

    /**
     * Deletes {@link District} if the validations are passed.
     * @param id ID of the {@link District}
     * @throws DomainEntityNotFoundException if {@link District} is not found.
     * @throws OperationNotAllowedException if the {@link District} is already deleted.
     * @throws OperationNotAllowedException if the {@link District} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = DISTRICTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        // TODO: 03.12.22 Check if there is no connected object to this nomenclature
        log.debug("Removing district with ID: {}", id);
        District district = districtRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-District not found, ID: " + id));

        if (district.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (districtRepository.getActiveConnectionsCount(id) > 0) {
            log.error("You can’t delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can’t delete the nomenclature because it is connected to another object in the system");
        }

        district.setStatus(DELETED);
        districtRepository.save(district);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return districtRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return districtRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link District} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = DISTRICTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the districts alphabetically");
        List<District> districts = districtRepository.orderByName();
        long orderingId = 1;

        for (District d : districts) {
            d.setOrderingId(orderingId);
            orderingId++;
        }

        districtRepository.saveAll(districts);
    }

    /**
     * Changes the ordering of a {@link District} item in the districts list to a specified position.
     * The method retrieves the {@link District} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link District} item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link District} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = DISTRICTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of district item with ID: {} to place {}", request.getId(), request.getOrderingId());

        District district = districtRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-District not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<District> districts;

        if (district.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = district.getOrderingId();
            districts = districtRepository.findInOrderingIdRange(
                    start,
                    end,
                    district.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (District d : districts) {
                d.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = district.getOrderingId();
            end = request.getOrderingId();
            districts = districtRepository.findInOrderingIdRange(
                    start,
                    end,
                    district.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (District d : districts) {
                d.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        district.setOrderingId(request.getOrderingId());
        districts.add(district);
        districtRepository.saveAll(districts);
    }

    /**
     * Returns information (ID, name) about the upper chain of the {@link District}:
     * <ul>
     *     <li>{@link PopulatedPlace}</li>
     *     <li>{@link Municipality}</li>
     *     <li>{@link Region}</li>
     *     <li>{@link Country}</li>
     * </ul>
     *
     * @param id ID of {@link District}
     * @return {@link DistrictTreeResponse}
     */
    public DistrictTreeResponse treeView(Long id) {
        log.debug("Querying district for tree view, ID: {}", id);
        if (!districtRepository.existsById(id)) {
            throw new DomainEntityNotFoundException("id-District not found, ID " + id);
        }
        return districtRepository.getDistrictTreeView(id);
    }
}
