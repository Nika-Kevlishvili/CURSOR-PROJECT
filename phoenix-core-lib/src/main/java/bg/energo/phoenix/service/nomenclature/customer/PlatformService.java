package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Platform;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.PlatformRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.PlatformResponse;
import bg.energo.phoenix.repository.nomenclature.customer.PlatformRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.PLATFORMS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformService implements NomenclatureBaseService {
    private final PlatformRepository platformRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PLATFORMS;
    }

    /**
     * Filters {@link Platform} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Platform}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<PlatformResponse> Page&lt;PlatformResponse&gt;} containing detailed information
     */
    public Page<PlatformResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering platforms list with request: {}", request.toString());
        Page<Platform> page = platformRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(PlatformResponse::new);
    }

    /**
     * Filters {@link Platform} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Platform}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PLATFORMS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering platforms nomenclature with request: {}", request.toString());
        return platformRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link Platform} at the end with the highest ordering ID.
     * If the request asks to save {@link Platform} as a default and a default {@link Platform} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link PlatformRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link PlatformResponse}
     */
    @Transactional
    public PlatformResponse add(PlatformRequest request) {
        log.debug("Adding platform: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (platformRepository.countPlatformByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Platform with the same name already exists;");
            throw new OperationNotAllowedException("name-Platform with the same name already exists;");
        }

        Long lastOrderingId = platformRepository.findLastOrderingId();
        Platform platform = new Platform(request);
        platform.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        if (request.getDefaultSelection()) {
            Optional<Platform> currentDefaultPlatformOptional = platformRepository.findByDefaultSelectionTrue();
            if (currentDefaultPlatformOptional.isPresent()) {
                Platform currentDefaultPlatform = currentDefaultPlatformOptional.get();
                currentDefaultPlatform.setDefaultSelection(false);
                platformRepository.save(currentDefaultPlatform);
            }
        }
        Platform platformEntity = platformRepository.save(platform);
        return new PlatformResponse(platformEntity);
    }

    /**
     * Retrieves detailed information about {@link Platform} by ID
     *
     * @param id ID of {@link Platform}
     * @return {@link PlatformResponse}
     * @throws DomainEntityNotFoundException if no {@link Platform} was found with the provided ID.
     */
    public PlatformResponse view(Long id) {
        log.debug("Fetching platform with ID: {}", id);
        Platform platform = platformRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Platform not found, ID: " + id));
        return new PlatformResponse(platform);
    }

    /**
     * Edit the requested {@link Platform}.
     * If the request asks to save {@link Platform} as a default and a default {@link Platform} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link Platform}
     * @param request {@link PlatformRequest}
     * @throws DomainEntityNotFoundException if {@link Platform} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link Platform} is deleted.
     * @return {@link PlatformResponse}
     */
    @Transactional
    public PlatformResponse edit(Long id, PlatformRequest request) {
        log.debug("Editing platform: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Platform platform = platformRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Platform not found, ID: " + id));

        if (platformRepository.countPlatformByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !platform.getName().equals(request.getName().trim())) {
            log.error("name-Platform with the same name already exists;");
            throw new OperationNotAllowedException("name-Platform with the same name already exists;");
        }

        if (platform.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !platform.isDefaultSelection()) {
            Optional<Platform> currentDefaultPlatformOptional = platformRepository.findByDefaultSelectionTrue();
            if (currentDefaultPlatformOptional.isPresent()) {
                Platform currentDefaultPlatform = currentDefaultPlatformOptional.get();
                currentDefaultPlatform.setDefaultSelection(false);
                platformRepository.save(currentDefaultPlatform);
            }
        }
        platform.setDefaultSelection(request.getDefaultSelection());

        platform.setName(request.getName().trim());
        platform.setStatus(request.getStatus());
        return new PlatformResponse(platformRepository.save(platform));
    }

    /**
     * Deletes {@link Platform} if the validations are passed.
     *
     * @param id ID of the {@link Platform}
     * @throws DomainEntityNotFoundException if {@link Platform} is not found.
     * @throws OperationNotAllowedException if the {@link Platform} is already deleted.
     * @throws OperationNotAllowedException if the {@link Platform} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PLATFORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing platform with ID: {}", id);
        Platform platform = platformRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Platform not found, ID: " + id));

        if (platform.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (platformRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        platform.setStatus(DELETED);
        platformRepository.save(platform);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return platformRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return platformRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link Platform} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PLATFORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the platforms alphabetically");
        List<Platform> platforms = platformRepository.orderByName();
        long orderingId = 1;

        for (Platform p : platforms) {
            p.setOrderingId(orderingId);
            orderingId++;
        }

        platformRepository.saveAll(platforms);
    }

    /**
     * Changes the ordering of a {@link Platform} item in the platforms list to a specified position.
     * The method retrieves the {@link Platform} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Platform} item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link Platform} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PLATFORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of platform item with ID: {} in platforms to place: {}", request.getId(), request.getOrderingId());

        Platform platform = platformRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Platform not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Platform> platforms;

        if (platform.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = platform.getOrderingId();

            platforms = platformRepository.findInOrderingIdRange(
                    start,
                    end,
                    platform.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Platform p : platforms) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = platform.getOrderingId();
            end = request.getOrderingId();

            platforms = platformRepository.findInOrderingIdRange(
                    start,
                    end,
                    platform.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Platform p : platforms) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        platform.setOrderingId(request.getOrderingId());
        platforms.add(platform);
        platformRepository.saveAll(platforms);
    }
}
