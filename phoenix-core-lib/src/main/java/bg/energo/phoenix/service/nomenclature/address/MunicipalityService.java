package bg.energo.phoenix.service.nomenclature.address;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.address.Country;
import bg.energo.phoenix.model.entity.nomenclature.address.District;
import bg.energo.phoenix.model.entity.nomenclature.address.Municipality;
import bg.energo.phoenix.model.entity.nomenclature.address.Region;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.MunicipalityFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.MunicipalityRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.MunicipalityDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.MunicipalityResponse;
import bg.energo.phoenix.repository.nomenclature.address.MunicipalityRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.MUNICIPALITIES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MunicipalityService implements NomenclatureBaseService {
    private final MunicipalityRepository municipalityRepository;
    private final RegionRepository regionRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.MUNICIPALITIES;
    }

    /**
     * Filters {@link Municipality} against the provided {@link MunicipalityFilterRequest}:
     * If regionId is provided in {@link MunicipalityFilterRequest}, only those items will be returned which belong to the requested {@link Region}.
     * If excludedItemId is provided in {@link MunicipalityFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link MunicipalityFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link MunicipalityFilterRequest}
     * @return {@link Page<MunicipalityResponse> Page&lt;MunicipalityResponse&gt;} containing detailed information
     */
    public Page<MunicipalityResponse> filter(MunicipalityFilterRequest request) {
        log.debug("Fetching municipalities list with request: {}", request.toString());
        Page<Municipality> page = municipalityRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getRegionId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(MunicipalityResponse::new);
    }

    /**
     * Filters {@link District} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
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
                    @PermissionMapping(context = MUNICIPALITIES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering municipalities list with request: {}", request);
        return municipalityRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link Municipality} at the end with the highest ordering ID.
     * If the request asks to save {@link Municipality} as a default and a default {@link Municipality} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link MunicipalityRequest}
     * @return {@link MunicipalityResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link Region} provided in the request is not found or is DELETED.
     */
    @Transactional
    public MunicipalityResponse add(MunicipalityRequest request) {
        log.debug("Adding municipality: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (municipalityRepository.countMunicipalityByStatusRegionAndName(request.getName(), request.getRegionId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-Municipality with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Municipality with the same name and parent already exists;");
        }

        Region region = regionRepository
                .findById(request.getRegionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("regionId-Region not found, ID: " + request.getRegionId()));


        if (region.getStatus().equals(DELETED)) {
            log.error("Cannot add municipality to DELETED region");
            throw new ClientException("regionId-Cannot add municipality to DELETED region", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = municipalityRepository.findLastOrderingId();
        Municipality municipality = new Municipality(request);
        municipality.setOrderingId(topId == null ? 1 : topId + 1);
        municipality.setRegion(region);

        if (request.getDefaultSelection()) {
            Optional<Municipality> currentDefaultMunicipalityOptional = municipalityRepository.findByDefaultSelectionTrue();
            if (currentDefaultMunicipalityOptional.isPresent()) {
                Municipality currentDefaultMunicipality = currentDefaultMunicipalityOptional.get();
                currentDefaultMunicipality.setDefaultSelection(false);
                municipalityRepository.save(currentDefaultMunicipality);
            }
        }

        return new MunicipalityResponse(municipalityRepository.save(municipality));
    }

    /**
     * Retrieves detailed information about {@link Municipality} by ID
     *
     * @param id ID of {@link Municipality}
     * @return {@link MunicipalityResponse}
     * @throws DomainEntityNotFoundException if no {@link Municipality} was found with the provided ID.
     */
    public MunicipalityResponse view(Long id) {
        log.debug("Fetching municipality with ID: {}", id);
        Municipality municipality = municipalityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Municipality not found, ID: " + id));
        return new MunicipalityResponse(municipality);
    }

    /**
     * Retrieves extended information about {@link Municipality} including information about its parent {@link Region}
     *
     * @param id ID of {@link Municipality}
     * @return {@link MunicipalityDetailedResponse}
     * @throws DomainEntityNotFoundException if no {@link Municipality} was found with the provided ID.
     */
    public MunicipalityDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed municipality with ID: {}", id);
        Municipality municipality = municipalityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Municipality not found, ID: " + id));
        return new MunicipalityDetailedResponse(municipality);
    }

    /**
     * Edits the requested {@link Municipality}.
     * If the request asks to save {@link Municipality} as a default and a default {@link Municipality} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Municipality}
     * @param request {@link MunicipalityRequest}
     * @return {@link MunicipalityResponse}
     * @throws DomainEntityNotFoundException if {@link Municipality} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Municipality} is deleted.
     * @throws DomainEntityNotFoundException if the {@link Region} provided in the request is not found.
     * @throws IllegalArgumentException      if the {@link Region} provided in the request has DELETED status.
     */
    @Transactional
    public MunicipalityResponse edit(Long id, MunicipalityRequest request) {
        log.debug("Editing municipality: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Municipality municipality = municipalityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Municipality not found, ID: " + id));

        if (municipalityRepository.countMunicipalityByStatusRegionAndName(request.getName(), request.getRegionId(), List.of(ACTIVE, INACTIVE), municipality.getId()) > 0) {
            log.error("name-Municipality with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-Municipality with the same name and parent already exists;");
        }

        if (municipality.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!municipality.getRegion().getId().equals(request.getRegionId())) {
            Region region = regionRepository
                    .findById(request.getRegionId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("regionId-Region not found, ID: " + request.getRegionId()));

            if (region.getStatus().equals(DELETED)) {
                log.error("Cannot add municipality to DELETED region");
                throw new ClientException("regionId-Cannot add municipality to DELETED region", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            municipality.setRegion(region);
        }

        if (request.getDefaultSelection() && !municipality.isDefaultSelection()) {
            Optional<Municipality> currentDefaultMunicipalityOptional = municipalityRepository.findByDefaultSelectionTrue();
            if (currentDefaultMunicipalityOptional.isPresent()) {
                Municipality currentDefaultMunicipality = currentDefaultMunicipalityOptional.get();
                currentDefaultMunicipality.setDefaultSelection(false);
                municipalityRepository.save(currentDefaultMunicipality);
            }
        }
        municipality.setDefaultSelection(request.getDefaultSelection());

        municipality.setName(request.getName().trim());
        municipality.setStatus(request.getStatus());
        return new MunicipalityResponse(municipalityRepository.save(municipality));
    }

    /**
     * Deletes {@link Municipality} if the validations are passed.
     *
     * @param id ID of the {@link Municipality}
     * @throws DomainEntityNotFoundException if {@link Municipality} is not found.
     * @throws OperationNotAllowedException  if the {@link Municipality} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Municipality} has active or inactive children.
     * @throws OperationNotAllowedException  if the {@link Municipality} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MUNICIPALITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing municipality with ID: {}", id);
        Municipality municipality = municipalityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Municipality not found, ID: " + id));

        if (municipality.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        Long childPopulatedPlacesCount = municipalityRepository
                .getPopulatedPlacesCountByStatusAndMunicipalityId(List.of(ACTIVE, INACTIVE), municipality.getId());
        if (childPopulatedPlacesCount > 0) {
            log.error("Item has active or inactive children.");
            throw new OperationNotAllowedException("id-Item has active or inactive children.");
        }

        if (municipalityRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        municipality.setStatus(DELETED);
        municipalityRepository.save(municipality);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return municipalityRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return municipalityRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link Municipality} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MUNICIPALITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the municipalities alphabetically");
        List<Municipality> municipalities = municipalityRepository.orderByName();
        long orderingId = 1;

        for (Municipality m : municipalities) {
            m.setOrderingId(orderingId);
            orderingId++;
        }

        municipalityRepository.saveAll(municipalities);
    }

    /**
     * Changes the ordering of a {@link Municipality} item in the municipalities list to a specified position.
     * The method retrieves the {@link Municipality} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Municipality} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Municipality} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MUNICIPALITIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of municipality item with ID: {} to place {}", request.getId(), request.getOrderingId());

        Municipality municipality = municipalityRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Municipality not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Municipality> municipalities;

        if (municipality.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = municipality.getOrderingId();
            municipalities = municipalityRepository.findInOrderingIdRange(
                    start,
                    end,
                    municipality.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Municipality m : municipalities) {
                m.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = municipality.getOrderingId();
            end = request.getOrderingId();
            municipalities = municipalityRepository.findInOrderingIdRange(
                    start,
                    end,
                    municipality.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Municipality m : municipalities) {
                m.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        municipality.setOrderingId(request.getOrderingId());
        municipalities.add(municipality);
        municipalityRepository.saveAll(municipalities);
    }
}
