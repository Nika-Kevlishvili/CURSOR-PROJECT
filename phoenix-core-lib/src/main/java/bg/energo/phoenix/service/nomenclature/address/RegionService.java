package bg.energo.phoenix.service.nomenclature.address;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.address.Country;
import bg.energo.phoenix.model.entity.nomenclature.address.Region;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.RegionFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.RegionRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.RegionDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.RegionResponse;
import bg.energo.phoenix.repository.nomenclature.address.CountryRepository;
import bg.energo.phoenix.repository.nomenclature.address.RegionRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.REGIONS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class RegionService implements NomenclatureBaseService {
    private final RegionRepository regionRepository;
    private final CountryRepository countryRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.REGIONS;
    }

    /**
     * Filters {@link Region} against the provided {@link RegionFilterRequest}:
     * If countryId is provided in {@link RegionFilterRequest}, only those items will be returned which belong to the requested {@link Country}.
     * If excludedItemId is provided in {@link RegionFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link RegionFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link RegionFilterRequest}
     * @return {@link Page<RegionResponse> Page&lt;RegionResponse&gt;} containing detailed information
     */
    public Page<RegionResponse> filter(RegionFilterRequest request) {
        log.debug("Fetching regions list with request: {}", request.toString());
        Page<Region> page = regionRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getCountryId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(RegionResponse::new);
    }

    /**
     * Filters {@link Region} against the provided {@link NomenclatureItemsBaseFilterRequest}:
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<NomenclatureResponse> Page&lt;NomenclatureResponse&gt;} containing detailed information
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REGIONS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering regions list with statuses: {}", request);
        return regionRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link Region} at the end with the highest ordering ID.
     * If the request asks to save {@link Region} as a default and a default {@link Region} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link RegionRequest}
     * @return {@link RegionResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link Country} provided in the request is not found or is DELETED.
     */
    @Transactional
    public RegionResponse add(RegionRequest request) {
        log.debug("Adding region: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (regionRepository.countRegionsByStatusCountryAndName(request.getName(), request.getCountryId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-Region with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Region with the same name and parent already exists;");
        }

        Country country = countryRepository
                .findById(request.getCountryId())
                .orElseThrow(() -> new DomainEntityNotFoundException("countryId-Country not found: " + request.getCountryId()));

        if (country.getStatus().equals(DELETED)) {
            log.error("Cannot add region to DELETED country");
            throw new ClientException("status-Cannot add region to DELETED country", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = regionRepository.findLastOrderingId();
        Region region = new Region(request);
        region.setOrderingId(topId == null ? 1 : topId + 1);
        region.setCountry(country);

        if (request.getDefaultSelection()) {
            Optional<Region> currentDefaultRegionOptional = regionRepository.findByDefaultSelectionTrue();
            if (currentDefaultRegionOptional.isPresent()) {
                Region currentDefaultRegion = currentDefaultRegionOptional.get();
                currentDefaultRegion.setDefaultSelection(false);
                regionRepository.save(currentDefaultRegion);
            }
        }

        return new RegionResponse(regionRepository.save(region));
    }

    /**
     * Retrieves detailed information about {@link Region} by ID
     *
     * @param id ID of {@link Region}
     * @return {@link RegionResponse}
     * @throws DomainEntityNotFoundException if no {@link Region} was found with the provided ID.
     */
    public RegionResponse view(Long id) {
        log.debug("Fetching region with ID: {}", id);
        Region region = regionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Region not found, ID: " + id));
        return new RegionResponse(region);
    }

    /**
     * Retrieves extended information about {@link Region} including information about its parent {@link Country}
     *
     * @param id ID of {@link Region}
     * @return {@link RegionDetailedResponse}
     * @throws DomainEntityNotFoundException if no {@link Region} was found with the provided ID.
     */
    public RegionDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed region with ID: {}", id);
        Region region = regionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Region not found, ID: " + id));
        return new RegionDetailedResponse(region);
    }

    /**
     * Edits the requested {@link Region}.
     * If the request asks to save {@link Region} as a default and a default {@link Region} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Region}
     * @param request {@link RegionRequest}
     * @return {@link RegionResponse}
     * @throws DomainEntityNotFoundException if {@link Region} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Region} is deleted.
     * @throws DomainEntityNotFoundException if the {@link Country} provided in the request is not found.
     * @throws IllegalArgumentException      if the {@link Country} provided in the request has DELETED status.
     */
    @Transactional
    public RegionResponse edit(Long id, RegionRequest request) {
        log.debug("Editing region: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Region region = regionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Region not found, ID: " + id));

        if (regionRepository.countRegionsByStatusCountryAndName(request.getName(), request.getCountryId(), List.of(ACTIVE, INACTIVE), region.getId()) > 0) {
            log.error("name-Region with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Region with the same name and parent already exists;");
        }


        if (region.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!region.getCountry().getId().equals(request.getCountryId())) {
            Country country = countryRepository
                    .findById(request.getCountryId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("countryId-Country not found: " + request.getCountryId()));

            if (country.getStatus().equals(DELETED)) {
                log.error("Cannot add region to DELETED country");
                throw new ClientException("countryId-Cannot add region to DELETED country", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            region.setCountry(country);
        }

        if (request.getDefaultSelection() && !region.isDefaultSelection()) {
            Optional<Region> currentDefaultRegionOptional = regionRepository.findByDefaultSelectionTrue();
            if (currentDefaultRegionOptional.isPresent()) {
                Region currentDefaultRegion = currentDefaultRegionOptional.get();
                currentDefaultRegion.setDefaultSelection(false);
                regionRepository.save(currentDefaultRegion);
            }
        }
        region.setDefaultSelection(request.getDefaultSelection());

        region.setName(request.getName().trim());
        region.setStatus(request.getStatus());
        return new RegionResponse(regionRepository.save(region));
    }

    /**
     * Deletes {@link Region} if the validations are passed.
     *
     * @param id ID of the {@link Region}
     * @throws DomainEntityNotFoundException if {@link Region} is not found.
     * @throws OperationNotAllowedException  if the {@link Region} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Region} has active or inactive children.
     * @throws OperationNotAllowedException  if the {@link Region} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REGIONS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public void delete(Long id) {
        log.debug("Removing region with ID: {}", id);
        Region region = regionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Region not found, ID: " + id));

        if (region.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        Long childMunicipalitiesCount = regionRepository
                .getMunicipalitiesCountByStatusAndRegionId(List.of(ACTIVE, INACTIVE), region.getId());
        if (childMunicipalitiesCount > 0) {
            log.error("Item has active or inactive children.");
            throw new OperationNotAllowedException("id-Item has active or inactive children.");
        }

        if (regionRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        region.setStatus(DELETED);
        regionRepository.save(region);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return regionRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return regionRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link Region} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REGIONS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the regions alphabetically");
        List<Region> regions = regionRepository.orderByName();
        long orderingId = 1;

        for (Region r : regions) {
            r.setOrderingId(orderingId);
            orderingId++;
        }

        regionRepository.saveAll(regions);
    }

    /**
     * Changes the ordering of a {@link Region} item in the regions list to a specified position.
     * The method retrieves the {@link Region} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Region} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Region} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REGIONS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of region item with ID: {} to place {}", request.getId(), request.getOrderingId());

        Region region = regionRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Region not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Region> regions;

        if (region.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = region.getOrderingId();
            regions = regionRepository.findInOrderingIdRange(
                    start,
                    end,
                    region.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Region r : regions) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = region.getOrderingId();
            end = request.getOrderingId();
            regions = regionRepository.findInOrderingIdRange(
                    start,
                    end,
                    region.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Region r : regions) {
                r.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        region.setOrderingId(request.getOrderingId());
        regions.add(region);
        regionRepository.saveAll(regions);
    }
}
