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
import bg.energo.phoenix.model.request.nomenclature.address.ZipCodeFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.address.ZipCodeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.ZipCodeDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.address.ZipCodeResponse;
import bg.energo.phoenix.model.response.nomenclature.address.tree.ZipCodeTreeResponse;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.ZipCodeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.ZIP_CODES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipCodeService implements NomenclatureBaseService {
    private final ZipCodeRepository zipCodeRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ZIP_CODES;
    }

    /**
     * Filters {@link ZipCode} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link ZipCode}'s name</li>
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
                    @PermissionMapping(context = ZIP_CODES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering zipcodes list with statuses: {}", request);
        return zipCodeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link ZipCode} item in the zip codes list to a specified position.
     * The method retrieves the {@link ZipCode} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ZipCode} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ZipCode} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ZIP_CODES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of zip code item with ID: {} to place {}", request.getId(), request.getOrderingId());

        ZipCode zipCode = zipCodeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ZipCode> zipCodes;

        if (zipCode.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = zipCode.getOrderingId();
            zipCodes = zipCodeRepository.findInOrderingIdRange(
                    start,
                    end,
                    zipCode.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ZipCode z : zipCodes) {
                z.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = zipCode.getOrderingId();
            end = request.getOrderingId();
            zipCodes = zipCodeRepository.findInOrderingIdRange(
                    start,
                    end,
                    zipCode.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ZipCode z : zipCodes) {
                z.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        zipCode.setOrderingId(request.getOrderingId());
        zipCodes.add(zipCode);
        zipCodeRepository.saveAll(zipCodes);
    }

    /**
     * Sorts all {@link ZipCode} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ZIP_CODES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the zip codes alphabetically");
        List<ZipCode> zipCodes = zipCodeRepository.orderByName();
        long orderingId = 1;

        for (ZipCode z : zipCodes) {
            z.setOrderingId(orderingId);
            orderingId++;
        }

        zipCodeRepository.saveAll(zipCodes);
    }

    /**
     * Deletes {@link ZipCode} if the validations are passed.
     *
     * @param id ID of the {@link ZipCode}
     * @throws DomainEntityNotFoundException if {@link ZipCode} is not found.
     * @throws OperationNotAllowedException  if the {@link ZipCode} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ZipCode} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ZIP_CODES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing zip code with ID: {}", id);
        ZipCode zipCode = zipCodeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Zip code not found"));

        if (zipCode.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        if (zipCodeRepository.getActiveConnectionsCount(id) > 0) {
            log.error("You can’t delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can’t delete the nomenclature because it is connected to another object in the system");
        }

        zipCode.setStatus(DELETED);
        zipCodeRepository.save(zipCode);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return zipCodeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return zipCodeRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link ZipCode} at the end with the highest ordering ID.
     * If the request asks to save {@link ZipCode} as a default and a default {@link ZipCode} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ZipCodeRequest}
     * @return {@link ZipCodeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if {@link PopulatedPlace} provided in the request is not found or is DELETED.
     */
    @Transactional
    public ZipCodeResponse add(ZipCodeRequest request) {
        log.debug("Adding zipcode: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (zipCodeRepository.countZipCodeByStatusCountryAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-ZipCode with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-ZipCode with the same name and parent already exists;");
        }

        PopulatedPlace populatedPlace = populatedPlaceRepository
                .findById(request.getPopulatedPlaceId())
                .orElseThrow(() -> new DomainEntityNotFoundException("populatedPlaceId-Populated place not found"));

        if (populatedPlace.getStatus().equals(DELETED)) {
            log.error("populatedPlaceId-Cannot add zip code to DELETED populated place");
            throw new ClientException("populatedPlaceId-Cannot add zip code to DELETED populated place", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long topId = zipCodeRepository.findLastOrderingId();
        ZipCode zipCode = new ZipCode(request);
        zipCode.setOrderingId(topId == null ? 1 : topId + 1);
        zipCode.setPopulatedPlace(populatedPlace);

        if (request.getDefaultSelection()) {
            Optional<ZipCode> currentDefaultZipCodeOptional = zipCodeRepository.findByDefaultSelectionTrue();
            if (currentDefaultZipCodeOptional.isPresent()) {
                ZipCode currentDefaultZipCode = currentDefaultZipCodeOptional.get();
                currentDefaultZipCode.setDefaultSelection(false);
                zipCodeRepository.save(currentDefaultZipCode);
            }
        }

        return new ZipCodeResponse(zipCodeRepository.save(zipCode));
    }

    /**
     * Retrieves extended information about {@link ZipCode} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link ZipCode}
     * @return {@link ZipCodeDetailedResponse}
     * @throws DomainEntityNotFoundException if no {@link ZipCode} was found with the provided ID.
     */
    @Transactional
    public ZipCodeResponse edit(Long id, ZipCodeRequest request) {
        log.debug("Editing zip code: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ZipCode zipCode = zipCodeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (zipCodeRepository.countZipCodeByStatusCountryAndName(request.getName(), request.getPopulatedPlaceId(), List.of(ACTIVE, INACTIVE), zipCode.getId()) > 0) {
            log.error("name-ZipCode with the same name and parent already exists;");
            throw new OperationNotAllowedException("name-ZipCode with the same name and parent already exists;");
        }

        if (zipCode.getStatus().equals(DELETED)) {
            log.error("status-Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Can not edit DELETED item.");
        }

        if (!zipCode.getPopulatedPlace().getId().equals(request.getPopulatedPlaceId())) {
            if (checkZipCodeIsAssignedToActiveRecord(zipCode.getId())) {
                log.error("populatedPlaceId-Zip code is assigned to active record, Updating Populated place is not allowed");
                throw new OperationNotAllowedException("populatedPlaceId-Zip code is assigned to active record, Updating Populated place is not allowed");
            }

            PopulatedPlace populatedPlace = populatedPlaceRepository
                    .findById(request.getPopulatedPlaceId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("populatedPlaceId-Populated Place not found"));

            if (populatedPlace.getStatus().equals(DELETED)) {
                log.error("status-Can not add zipcode to DELETED populated place");
                throw new ClientException("status-Can not add zipcode to DELETED populated place", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            zipCode.setPopulatedPlace(populatedPlace);
        }

        if (request.getDefaultSelection() && !zipCode.isDefaultSelection()) {
            Optional<ZipCode> currentDefaultZipCodeOptional = zipCodeRepository.findByDefaultSelectionTrue();
            if (currentDefaultZipCodeOptional.isPresent()) {
                ZipCode currentDefaultZipCode = currentDefaultZipCodeOptional.get();
                currentDefaultZipCode.setDefaultSelection(false);
                zipCodeRepository.save(currentDefaultZipCode);
            }
        }
        zipCode.setDefaultSelection(request.getDefaultSelection());

        zipCode.setName(request.getName().trim());
        zipCode.setStatus(request.getStatus());
        return new ZipCodeResponse(zipCodeRepository.save(zipCode));
    }

    public boolean checkZipCodeIsAssignedToActiveRecord(Long id) {
        return zipCodeRepository.getActiveConnectionsCount(id) > 0;
    }

    /**
     * Retrieves detailed information about {@link ZipCode} by ID
     *
     * @param id ID of {@link ZipCode}
     * @return {@link ZipCodeResponse}
     * @throws DomainEntityNotFoundException if no {@link ZipCode} was found with the provided ID.
     */
    public ZipCodeResponse view(Long id) {
        log.debug("Fetching zip code with ID: {}", id);
        ZipCode zipCode = zipCodeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ZipCode not found;"));
        return new ZipCodeResponse(zipCode);
    }

    /**
     * Retrieves extended information about {@link ZipCode} including information about its parent {@link PopulatedPlace}
     *
     * @param id ID of {@link ZipCode}
     * @return {@link ZipCodeDetailedResponse}
     * @throws DomainEntityNotFoundException if no {@link ZipCode} was found with the provided ID.
     */
    public ZipCodeDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed zip code with ID: {}", id);
        ZipCode zipCode = zipCodeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ZipCode not found"));
        return new ZipCodeDetailedResponse(zipCode);
    }

    /**
     * Filters {@link ZipCode} against the provided {@link ZipCodeFilterRequest}:
     * If populatedPlaceId is provided in {@link ZipCodeFilterRequest}, only those items will be returned which belong to the requested {@link PopulatedPlace}.
     * If excludedItemId is provided in {@link ZipCodeFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link ZipCodeFilterRequest}, the searchable field are:
     * <ul>
     *     <li>{@link ZipCode}'s name</li>
     *     <li>{@link PopulatedPlace}'s name.</li>
     *     <li>{@link Municipality}'s name.</li>
     *     <li>{@link Region}'s name.</li>
     *     <li>{@link Country}'s name</li>
     * </ul>
     *
     * @param request {@link ZipCodeFilterRequest}
     * @return {@link Page<ZipCodeResponse> Page&lt;ZipCodeResponse&gt;} containing detailed information
     */
    public Page<ZipCodeResponse> filter(ZipCodeFilterRequest request) {
        log.debug("Fetching zip codes list with request: {}", request.toString());
        Page<ZipCode> page = zipCodeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getPopulatedPlaceId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(ZipCodeResponse::new);
    }

    /**
     * Returns information (ID, name) about the upper chain of the {@link ZipCode}:
     * <ul>
     *     <li>{@link PopulatedPlace}</li>
     *     <li>{@link Municipality}</li>
     *     <li>{@link Region}</li>
     *     <li>{@link Country}</li>
     * </ul>
     *
     * @param id ID of {@link ZipCode}
     * @return {@link ZipCodeTreeResponse}
     */
    public ZipCodeTreeResponse treeView(Long id) {
        log.debug("Querying zip codes for tree view, ID: {}", id);
        if (!zipCodeRepository.existsById(id)) {
            throw new DomainEntityNotFoundException("id-Zip code not found, ID " + id);
        }
        return zipCodeRepository.getZipCodeTreeView(id);
    }
}
