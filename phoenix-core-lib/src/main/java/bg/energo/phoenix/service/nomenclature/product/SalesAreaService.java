package bg.energo.phoenix.service.nomenclature.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.SalesArea;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.SalesAreaRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import bg.energo.phoenix.repository.nomenclature.product.SalesAreaRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.service.nomenclature.mapper.SalesAreaMapper;
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
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.SALES_AREAS;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SALE_AREAS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAreaService implements NomenclatureBaseService {

    private final SalesAreaMapper salesAreaMapper;
    private final SalesAreaRepository salesAreaRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return SALES_AREAS;
    }

    /**
     * Filters {@link SalesArea} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SalesArea#name}</li>
     *     <li>{@link SalesArea#loginPortalTag}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_AREAS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sales areas with statuses: {}", request);
        Page<SalesArea> salesAreas = salesAreaRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
        return salesAreas.map(salesAreaMapper::nomenclatureResponseFromEntity);
    }

    /**
     * Changes the ordering of a {@link SalesArea} item in the SalesArea list to a specified position.
     * The method retrieves the {@link SalesArea} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link SalesArea} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link SalesArea} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in sales areas", request.getId());

        SalesArea salesArea = salesAreaRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales area not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<SalesArea> salesAreas;

        if (salesArea.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = salesArea.getOrderingId();
            salesAreas = salesAreaRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            salesArea.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (SalesArea sa : salesAreas) {
                sa.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = salesArea.getOrderingId();
            end = request.getOrderingId();
            salesAreas = salesAreaRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            salesArea.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (SalesArea sa : salesAreas) {
                sa.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        salesArea.setOrderingId(request.getOrderingId());
        salesAreas.add(salesArea);
        salesAreaRepository.saveAll(salesAreas);
    }

    /**
     * Sorts all {@link SalesArea} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the sales areas alphabetically");

        List<SalesArea> salesAreas = salesAreaRepository.orderByName();
        long orderingId = 1;

        for (SalesArea sa : salesAreas) {
            sa.setOrderingId(orderingId);
            orderingId++;
        }

        salesAreaRepository.saveAll(salesAreas);
    }

    /**
     * Deletes {@link SalesArea} if the validations are passed.
     *
     * @param id ID of the {@link SalesArea}
     * @throws DomainEntityNotFoundException if {@link SalesArea} is not found.
     * @throws OperationNotAllowedException if the {@link SalesArea} is already deleted.
     * @throws OperationNotAllowedException if the {@link SalesArea} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_AREAS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing sales area with ID: {}", id);

        SalesArea salesArea = salesAreaRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales area not found, ID: " + id));

        if (salesArea.getStatus().equals(DELETED)) {
            log.error("Sales area {} is already deleted", id);
            throw new OperationNotAllowedException("id-Sales area " + id + " is already deleted");
        }

        Long activeConnections = salesAreaRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE),
                List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        salesArea.setStatus(DELETED);
        salesAreaRepository.save(salesArea);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return salesAreaRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return salesAreaRepository.findByIdIn(ids);
    }

    /**
     * Filters {@link SalesArea} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SalesArea#name}</li>
     *     <li>{@link SalesArea#loginPortalTag}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<SalesAreaResponse> Page&lt;SalesAreaResponse&gt;} containing detailed information
     */
    public Page<SalesAreaResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sales areas list with request: {}", request);
        Page<SalesArea> salesAreas = salesAreaRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return salesAreas.map(salesAreaMapper::responseFromEntity);
    }

    /**
     * Adds {@link SalesArea} at the end with the highest ordering ID.
     * If the request asks to save {@link SalesArea} as a default and a default {@link SalesArea} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link SalesAreaRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link SalesAreaResponse}
     */
    @Transactional
    public SalesAreaResponse add(SalesAreaRequest request) {
        log.debug("Adding sales area: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (salesAreaRepository.countSalesAreaByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Sales area with such name already exists");
            throw new ClientException("name-Sales area with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (!StringUtils.isEmpty(request.getLoginPortalTag())) {
            if (StringUtils.isEmpty(request.getLoginPortalTag().trim())) {
                log.error("Login portal tag name must not contain only spaces");
                throw new ClientException("loginPortalTag-Login portal tag name must not contain only spaces", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        Long lastOrderingId = salesAreaRepository.findLastOrderingId();
        SalesArea salesArea = salesAreaMapper.entityFromRequest(request);
        salesArea.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, salesArea);

        salesAreaRepository.save(salesArea);
        return salesAreaMapper.responseFromEntity(salesArea);
    }

    /**
     * Sets the default selection flag for the given {@link SalesArea} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link SalesArea} is set to false,
     * and the given {@link SalesArea} is set as the new default selection.
     *
     * @param request the {@link SalesAreaRequest} containing the status and default selection flag to use when setting the default selection
     * @param salesArea the {@link SalesArea} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(SalesAreaRequest request, SalesArea salesArea) {
        if (request.getStatus().equals(INACTIVE)) {
            salesArea.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<SalesArea> currentDefaultSalesAreaOptional = salesAreaRepository.findByDefaultSelectionTrue();
                if (currentDefaultSalesAreaOptional.isPresent()) {
                    SalesArea currentDefaultSalesArea = currentDefaultSalesAreaOptional.get();
                    currentDefaultSalesArea.setDefaultSelection(false);
                    salesAreaRepository.save(currentDefaultSalesArea);
                }
            }
            salesArea.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Retrieves detailed information about {@link SalesArea} by ID
     *
     * @param id ID of {@link SalesArea}
     * @return {@link SalesAreaResponse}
     * @throws DomainEntityNotFoundException if no {@link SalesArea} was found with the provided ID.
     */
    public SalesAreaResponse view(Long id) {
        log.debug("Fetching sales area with ID: {}", id);
        SalesArea salesArea = salesAreaRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales area not found, ID: " + id));
        return salesAreaMapper.responseFromEntity(salesArea);
    }

    /**
     * Edit the requested {@link SalesArea}.
     * If the request asks to save {@link SalesArea} as a default and a default {@link SalesArea} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * If the status inside request is {@link NomenclatureItemStatus#INACTIVE}, default selection will be set false,
     * no matter the requested default selection.
     *
     * @param id ID of {@link SalesArea}
     * @param request {@link SalesAreaRequest}
     * @throws DomainEntityNotFoundException if {@link SalesArea} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link SalesArea} is deleted.
     * @return {@link SalesAreaResponse}
     */
    @Transactional
    public SalesAreaResponse edit(Long id, SalesAreaRequest request) {
        log.debug("Editing sales area: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        SalesArea salesArea = salesAreaRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales area not found, ID: " + id));

        if (salesArea.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (!StringUtils.isEmpty(request.getLoginPortalTag())) {
            if (StringUtils.isEmpty(request.getLoginPortalTag().trim())) {
                log.error("Login portal tag name must not contain only spaces");
                throw new ClientException("loginPortalTag-Login portal tag name must not contain only spaces", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (salesAreaRepository.countSalesAreaByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !salesArea.getName().equals(request.getName().trim())) {
            log.error("Sales area with such name already exists");
            throw new ClientException("name-Sales area with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, salesArea);

        salesArea.setName(request.getName().trim());
        salesArea.setLoginPortalTag(request.getLoginPortalTag() == null ? null : request.getLoginPortalTag().trim());
        salesArea.setStatus(request.getStatus());

        return salesAreaMapper.responseFromEntity(salesArea);
    }

    /**
     * Sets the default selection flag for the given {@link SalesArea} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link SalesArea} is set to false,
     * and the given {@link SalesArea} is set as the new default selection.
     *
     * @param request the {@link SalesAreaRequest} containing the status and default selection flag to use when setting the default selection
     * @param salesArea the {@link SalesArea} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(SalesAreaRequest request, SalesArea salesArea) {
        if (request.getStatus().equals(INACTIVE)) {
            salesArea.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!salesArea.isDefaultSelection()) {
                    Optional<SalesArea> currentDefaultSalesAreaOptional = salesAreaRepository.findByDefaultSelectionTrue();
                    if (currentDefaultSalesAreaOptional.isPresent()) {
                        SalesArea currentDefaultSalesArea = currentDefaultSalesAreaOptional.get();
                        currentDefaultSalesArea.setDefaultSelection(false);
                        salesAreaRepository.save(currentDefaultSalesArea);
                    }
                }
            }
            salesArea.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
