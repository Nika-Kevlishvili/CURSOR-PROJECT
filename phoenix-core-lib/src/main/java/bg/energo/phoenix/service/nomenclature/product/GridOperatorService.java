package bg.energo.phoenix.service.nomenclature.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.GridOperatorRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.service.nomenclature.mapper.GridOperatorMapper;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.GRID_OPERATOR;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridOperatorService implements NomenclatureBaseService {

    private final GridOperatorMapper gridOperatorMapper;
    private final GridOperatorRepository gridOperatorRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.GRID_OPERATORS;
    }

    /**
     * Filters {@link GridOperator} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link GridOperator#name}</li>
     *     <li>{@link GridOperator#fullName}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GRID_OPERATOR, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering grid operators with request: {}", request);
        Page<GridOperator> gridOperators = gridOperatorRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
        return gridOperators.map(gridOperatorMapper::nomenclatureResponseFromEntity);
    }

    /**
     * Changes the ordering of a {@link GridOperator} item in the list to a specified position.
     * The method retrieves the {@link GridOperator} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link GridOperator} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link GridOperator} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in grid operators", request.getId());

        GridOperator gridOperator = gridOperatorRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Grid operator not found by ID: " + request.getId()));

        Long start;
        Long end;
        List<GridOperator> gridOperators;

        if (gridOperator.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = gridOperator.getOrderingId();
            gridOperators = gridOperatorRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            gridOperator.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GridOperator go : gridOperators) {
                go.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = gridOperator.getOrderingId();
            end = request.getOrderingId();
            gridOperators = gridOperatorRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            gridOperator.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (GridOperator go : gridOperators) {
                go.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        gridOperator.setOrderingId(request.getOrderingId());
        gridOperators.add(gridOperator);
        gridOperatorRepository.saveAll(gridOperators);
    }

    /**
     * Sorts all {@link GridOperator} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the grid operators alphabetically");

        List<GridOperator> gridOperators = gridOperatorRepository.orderByName();
        long orderingId = 1;

        for (GridOperator go : gridOperators) {
            go.setOrderingId(orderingId);
            orderingId++;
        }

        gridOperatorRepository.saveAll(gridOperators);
    }

    /**
     * Deletes {@link GridOperator} if the validations are passed.
     *
     * @param id ID of the {@link GridOperator}
     * @throws DomainEntityNotFoundException if {@link GridOperator} is not found.
     * @throws OperationNotAllowedException  if the {@link GridOperator} is already deleted.
     * @throws OperationNotAllowedException  if the {@link GridOperator} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing grid operator with ID: {}", id);

        GridOperator gridOperator = gridOperatorRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Grid operator not found, ID: " + id));

        if (gridOperator.getStatus().equals(DELETED)) {
            log.error("Grid operator {} is already deleted", id);
            throw new OperationNotAllowedException("id-Grid operator " + id + " is already deleted");
        }
        if(gridOperator.getIsHardCoded()){
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id: %s -Can't delete the hardcoded nomenclature;".formatted(gridOperator.getId()));
        }

        Long activeConnections = gridOperatorRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE),
                List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE),
                List.of(PodStatus.ACTIVE),
                List.of(MeterStatus.ACTIVE)
        );

        if (activeConnections > 0) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        gridOperator.setStatus(DELETED);
        gridOperatorRepository.save(gridOperator);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return gridOperatorRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return gridOperatorRepository.findByIdIn(ids);
    }

    /**
     * Filters {@link GridOperator} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link GridOperator#name}</li>
     *     <li>{@link GridOperator#fullName}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<GridOperatorResponse> Page&lt;GridOperatorResponse&gt;} containing detailed information
     */
    public Page<GridOperatorResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering grid operators list with request: {}", request);
        Page<GridOperator> gridOperators = gridOperatorRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return gridOperators.map(gridOperatorMapper::responseFromEntity);
    }

    /**
     * Adds {@link GridOperator} at the end with the highest ordering ID.
     * If the request asks to save {@link GridOperator} as a default and a default {@link GridOperator} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link GridOperatorRequest}
     * @return {@link GridOperatorResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public GridOperatorResponse add(GridOperatorRequest request) {
        log.debug("Adding grid operator: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (gridOperatorRepository.countGridOperatorsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Grid operator with such name already exists");
            throw new ClientException("name-Grid operator with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = gridOperatorRepository.findLastOrderingId();
        GridOperator gridOperator = gridOperatorMapper.entityFromRequest(request);
        gridOperator.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, gridOperator);

        gridOperator.setIsHardCoded(false);
        gridOperatorRepository.save(gridOperator);
        return gridOperatorMapper.responseFromEntity(gridOperator);
    }

    /**
     * Sets the default selection flag for the given {@link GridOperator} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link GridOperator} is set to false,
     * and the given {@link GridOperator} is set as the new default selection.
     *
     * @param request      the {@link GridOperatorRequest} containing the status and default selection flag to use when setting the default selection
     * @param gridOperator the {@link GridOperator} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(GridOperatorRequest request, GridOperator gridOperator) {
        if (request.getStatus().equals(INACTIVE)) {
            gridOperator.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<GridOperator> currentDefaultGridOperatorOptional = gridOperatorRepository.findByDefaultSelectionTrue();
                if (currentDefaultGridOperatorOptional.isPresent()) {
                    GridOperator currentDefaultGridOperator = currentDefaultGridOperatorOptional.get();
                    currentDefaultGridOperator.setDefaultSelection(false);
                    gridOperatorRepository.save(currentDefaultGridOperator);
                }
            }
            gridOperator.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Retrieves detailed information about {@link GridOperator} by ID
     *
     * @param id ID of {@link GridOperator}
     * @return {@link GridOperatorResponse}
     * @throws DomainEntityNotFoundException if no {@link GridOperator} was found with the provided ID.
     */
    public GridOperatorResponse view(Long id) {
        log.debug("Fetching grid operator with ID: {}", id);
        GridOperator gridOperator = gridOperatorRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Grid operator not found, ID: " + id));
        return gridOperatorMapper.responseFromEntity(gridOperator);
    }

    /**
     * Edit the requested {@link GridOperator}.
     * If the request asks to save {@link GridOperator} as a default and a default {@link GridOperator} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * If the status inside request is {@link NomenclatureItemStatus#INACTIVE}, default selection will be set false,
     * no matter the requested default selection.
     *
     * @param id      ID of {@link GridOperator}
     * @param request {@link GridOperatorRequest}
     * @return {@link GridOperatorResponse}
     * @throws DomainEntityNotFoundException if {@link GridOperator} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link GridOperator} is deleted.
     */
    @Transactional
    public GridOperatorResponse edit(Long id, GridOperatorRequest request) {
        log.debug("Editing grid operator: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GridOperator gridOperator = gridOperatorRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Grid operator not found, ID: " + id));

        if (gridOperator.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if(gridOperator.getIsHardCoded()){
            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be changed.;");
        }

        if (gridOperatorRepository.countGridOperatorsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !gridOperator.getName().equals(request.getName().trim())) {
            log.error("Grid operator with such name already exists");
            throw new ClientException("name-Grid operator with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, gridOperator);

        String terminationEmail = request.getPowerSupplyTerminationRequestEmail();
        String reconnectionEmail = request.getPowerSupplyReconnectionRequestEmail();
        String changeCBGEmail = request.getObjectionToChangeCBGEmail();
        Boolean isOwnedByEnergoPro = Objects.requireNonNullElse(request.getOwnedByEnergoPro(), false);

        gridOperator.setName(request.getName().trim());
        gridOperator.setFullName(request.getFullName().trim());
        gridOperator.setPowerSupplyTerminationRequestEmail(StringUtils.isEmpty(terminationEmail) ? null : terminationEmail.trim());
        gridOperator.setPowerSupplyReconnectionRequestEmail(StringUtils.isEmpty(reconnectionEmail) ? null : reconnectionEmail.trim());
        gridOperator.setObjectionToChangeCBGEmail(StringUtils.isEmpty(changeCBGEmail) ? null : changeCBGEmail.trim());
        gridOperator.setCodeForXEnergy(Objects.isNull(request.getCodeForXEnergy()) ? request.getCodeForXEnergy() : request.getCodeForXEnergy().replaceFirst("^0+", ""));
        gridOperator.setGridOperatorCode(Objects.isNull(request.getGridOperatorCode()) ? request.getGridOperatorCode() : request.getGridOperatorCode().replaceFirst("^0+", ""));
        gridOperator.setStatus(request.getStatus());
        gridOperator.setOwnedByEnergoPro(isOwnedByEnergoPro);

        gridOperatorRepository.save(gridOperator);
        return gridOperatorMapper.responseFromEntity(gridOperator);
    }

    /**
     * Sets the default selection flag for the given {@link GridOperator} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link GridOperator} is set to false,
     * and the given {@link GridOperator} is set as the new default selection.
     *
     * @param request      the {@link GridOperatorRequest} containing the status and default selection flag to use when setting the default selection
     * @param gridOperator the {@link GridOperator} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(GridOperatorRequest request, GridOperator gridOperator) {
        if (request.getStatus().equals(INACTIVE)) {
            gridOperator.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!gridOperator.isDefaultSelection()) {
                    Optional<GridOperator> currentDefaultGridOperatorOptional = gridOperatorRepository.findByDefaultSelectionTrue();
                    if (currentDefaultGridOperatorOptional.isPresent()) {
                        GridOperator currentDefaultGridOperator = currentDefaultGridOperatorOptional.get();
                        currentDefaultGridOperator.setDefaultSelection(false);
                        gridOperatorRepository.save(currentDefaultGridOperator);
                    }
                }
            }
            gridOperator.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
