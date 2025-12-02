package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.GccConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.GccConnectionTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.GccConnectionTypeResponse;
import bg.energo.phoenix.repository.nomenclature.customer.GccConnectionTypeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.GCC;
import static bg.energo.phoenix.permissions.PermissionContextEnum.GCC_CONNECTION_TYPE;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GccConnectionTypeService implements NomenclatureBaseService {
    private final GccConnectionTypeRepository gccConnectionTypeRepository;

    /**
     * Filters {@link GccConnectionType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GccConnectionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<GccConnectionTypeResponse> Page&lt;GccConnectionTypeResponse&gt;} containing detailed information
     */
    public Page<GccConnectionTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering GCC connection types list with request: {}", request.toString());
        Page<GccConnectionType> page = gccConnectionTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(GccConnectionTypeResponse::new);
    }

    /**
     * Filters {@link GccConnectionType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GccConnectionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GCC_CONNECTION_TYPE, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = GCC, permissions = {
                            GCC_VIEW_BASIC,
                            GCC_VIEW_DELETED
                    }),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering GccConnectionType list with statuses: {}", request);
        return gccConnectionTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link GccConnectionType} at the end with the highest ordering ID.
     * If the request asks to save {@link GccConnectionType} as a default and a default {@link GccConnectionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link GccConnectionType}
     * @return {@link GccConnectionTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public GccConnectionTypeResponse add(GccConnectionTypeRequest request) {
        log.debug("Adding GccConnectionType: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (gccConnectionTypeRepository.countGccConnectionTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-GccConnectionType with the same name already exists;");
            throw new OperationNotAllowedException("name-GccConnectionType with the same name already exists;");
        }

        Long lastSortOrder = gccConnectionTypeRepository.findLastOrderingId();
        GccConnectionType gccConnectionType = new GccConnectionType(request);
        gccConnectionType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<GccConnectionType> currentDefaultGccConnectionOptional =
                    gccConnectionTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultGccConnectionOptional.isPresent()) {
                GccConnectionType currentDefaultGccConnection = currentDefaultGccConnectionOptional.get();
                currentDefaultGccConnection.setDefaultSelection(false);
                gccConnectionTypeRepository.save(currentDefaultGccConnection);
            }
        }
        GccConnectionType GccConnectionTypeEntity = gccConnectionTypeRepository.save(gccConnectionType);
        return new GccConnectionTypeResponse(GccConnectionTypeEntity);
    }

    /**
     * Retrieves detailed information about {@link GccConnectionType} by ID
     *
     * @param id ID of {@link GccConnectionType}
     * @return {@link GccConnectionTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link GccConnectionType} was found with the provided ID.
     */
    public GccConnectionTypeResponse view(Long id) {
        log.debug("Fetching gccConnectionType with ID: {}", id);
        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new GccConnectionTypeResponse(gccConnectionType);
    }

    /**
     * Edit the requested {@link GccConnectionType}.
     * If the request asks to save {@link GccConnectionType} as a default and a default {@link GccConnectionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link GccConnectionType}
     * @param request {@link GccConnectionTypeRequest}
     * @return {@link GccConnectionTypeResponse}
     * @throws DomainEntityNotFoundException if {@link GccConnectionType} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link GccConnectionType} is deleted.
     */
    @Transactional
    public GccConnectionTypeResponse edit(Long id, GccConnectionTypeRequest request) {
        log.debug("Editing GccConnectionType: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (gccConnectionTypeRepository.countGccConnectionTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !gccConnectionType.getName().equals(request.getName().trim())) {
            log.error("name-GccConnectionType with the same name already exists;");
            throw new OperationNotAllowedException("name-GccConnectionType with the same name already exists;");
        }

        if (gccConnectionType.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !gccConnectionType.isDefaultSelection()) {
            Optional<GccConnectionType> currentDefaultGccConnectionOptional = gccConnectionTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultGccConnectionOptional.isPresent()) {
                GccConnectionType currentDefaultGccConnection = currentDefaultGccConnectionOptional.get();
                currentDefaultGccConnection.setDefaultSelection(false);
                gccConnectionTypeRepository.save(currentDefaultGccConnection);
            }
        }
        gccConnectionType.setDefaultSelection(request.getDefaultSelection());

        gccConnectionType.setName(request.getName().trim());
        gccConnectionType.setStatus(request.getStatus());
        return new GccConnectionTypeResponse(gccConnectionTypeRepository.save(gccConnectionType));
    }

    /**
     * Deletes {@link GccConnectionType} if the validations are passed.
     *
     * @param id ID of the {@link GccConnectionType}
     * @throws DomainEntityNotFoundException if {@link GccConnectionType} is not found.
     * @throws OperationNotAllowedException  if the {@link GccConnectionType} is already deleted.
     * @throws OperationNotAllowedException  if the {@link GccConnectionType} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GCC_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        // TODO: 26.11.22 Check if there is no connected object to this nomenclature item in system
        log.debug("Removing GccConnectionType with ID: {}", id);
        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (gccConnectionType.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (gccConnectionTypeRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        gccConnectionType.setStatus(DELETED);
        gccConnectionTypeRepository.save(gccConnectionType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return gccConnectionTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return gccConnectionTypeRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link GccConnectionType} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GCC_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the GccConnectionTypes alphabetically");
        List<GccConnectionType> gccConnectionTypesList = gccConnectionTypeRepository.orderByName();
        long orderingId = 1;

        for (GccConnectionType c : gccConnectionTypesList) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        gccConnectionTypeRepository.saveAll(gccConnectionTypesList);
    }

    /**
     * Changes the ordering of a {@link GccConnectionType} item in the GccConnectionType list to a specified position.
     * The method retrieves the {@link GccConnectionType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link GccConnectionType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link GccConnectionType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GCC_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in GccConnectionTypes to top", request.getId());

        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<GccConnectionType> gccConnectionTypes;

        if (gccConnectionType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = gccConnectionType.getOrderingId();
            gccConnectionTypes = gccConnectionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    gccConnectionType.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GccConnectionType c : gccConnectionTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = gccConnectionType.getOrderingId();
            end = request.getOrderingId();
            gccConnectionTypes = gccConnectionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    gccConnectionType.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (GccConnectionType c : gccConnectionTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        gccConnectionType.setOrderingId(request.getOrderingId());
        gccConnectionTypeRepository.save(gccConnectionType);
        gccConnectionTypeRepository.saveAll(gccConnectionTypes);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.GCC_CONNECTION_TYPE;
    }
}
