package bg.energo.phoenix.service.nomenclature.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.SalesChannelRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.nomenclature.product.SalesChannelRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.service.nomenclature.mapper.SalesChannelMapper;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.SALE_CHANNELS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesChannelService implements NomenclatureBaseService {

    private final SalesChannelRepository salesChannelRepository;
    private final SalesChannelMapper salesChannelMapper;
    private final PortalTagRepository portalTagRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SALES_CHANNELS;
    }

    /**
     * Filters {@link SalesChannel} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SalesChannel#name}</li>
     *     <li>{@link SalesChannel#loginPortalTag}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_CHANNELS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sales channels with statuses: {}", request);
return salesChannelRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }


    /**
     * Changes the ordering of a {@link SalesChannel} item in the list to a specified position.
     * The method retrieves the {@link SalesChannel} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link SalesChannel} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link SalesChannel} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_CHANNELS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in sales channels", request.getId());

        SalesChannel salesChannel = salesChannelRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales channel not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<SalesChannel> salesChannels;

        if (salesChannel.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = salesChannel.getOrderingId();
            salesChannels = salesChannelRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            salesChannel.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (SalesChannel sa : salesChannels) {
                sa.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = salesChannel.getOrderingId();
            end = request.getOrderingId();
            salesChannels = salesChannelRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            salesChannel.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (SalesChannel sa : salesChannels) {
                sa.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        salesChannel.setOrderingId(request.getOrderingId());
        salesChannels.add(salesChannel);
        salesChannelRepository.saveAll(salesChannels);
    }

    /**
     * Sorts all {@link SalesChannel} alphabetically not taking its status into consideration.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_CHANNELS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Transactional
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the sales channels alphabetically");

        List<SalesChannel> salesChannels = salesChannelRepository.orderByName();
        long orderingId = 1;

        for (SalesChannel sch : salesChannels) {
            sch.setOrderingId(orderingId);
            orderingId++;
        }

        salesChannelRepository.saveAll(salesChannels);
    }

    /**
     * Deletes {@link SalesChannel} if the validations are passed.
     *
     * @param id ID of the {@link SalesChannel}
     * @throws DomainEntityNotFoundException if {@link SalesChannel} is not found.
     * @throws OperationNotAllowedException if the {@link SalesChannel} is already deleted.
     * @throws OperationNotAllowedException if the {@link SalesChannel} is connected to active object.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SALE_CHANNELS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Transactional
    @Override
    public void delete(Long id) {
        log.debug("Removing sales channel with ID: {}", id);

        SalesChannel salesChannel = salesChannelRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales channel not found, ID: " + id));

        if (salesChannel.getStatus().equals(DELETED)) {
            log.error("Sales channel {} is already deleted", id);
            throw new OperationNotAllowedException("id-Sales channel " + id + " is already deleted");
        }

        Long activeConnections = salesChannelRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE),
                List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        salesChannel.setStatus(DELETED);
        salesChannelRepository.save(salesChannel);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return salesChannelRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return salesChannelRepository.findByIdIn(ids);
    }

    /**
     * Filters {@link SalesChannel} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link SalesChannel#name}</li>
     *     <li>{@link SalesChannel#loginPortalTag}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<SalesChannelResponse> Page&lt;SalesChannelResponse&gt;} containing detailed information
     */
    public Page<SalesChannelResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sales channels list with request: {}", request);
       return salesChannelRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }

    /**
     * Adds {@link SalesChannel} at the end with the highest ordering ID.
     * If the request asks to save {@link SalesChannel} as a default and a default {@link SalesChannel} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link SalesChannelRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link SalesChannelResponse}
     */
    @Transactional
    public SalesChannelResponse add(SalesChannelRequest request) {
        log.debug("Adding sales channel: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (salesChannelRepository.countSalesChannelByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Sales channel with such name already exists");
            throw new ClientException("name-Sales channel with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        PortalTagResponse tagResponse=null;
        if(request.getPortalTagId()!=null){
            tagResponse=new PortalTagResponse(portalTagRepository.findByIdAndStatus(request.getPortalTagId(), EntityStatus.ACTIVE)
                    .orElseThrow(()-> new DomainEntityNotFoundException("portalTagId-Portal tag does not exist!;")));
        }
        Long lastOrderingId = salesChannelRepository.findLastOrderingId();
        SalesChannel salesChannel = salesChannelMapper.entityFromRequest(request);
        salesChannel.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, salesChannel);

        salesChannelRepository.save(salesChannel);
        return salesChannelMapper.responseFromEntity(salesChannel,tagResponse);
    }

    /**
     * Sets the default selection flag for the given {@link SalesChannel} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link SalesChannel} is set to false,
     * and the given {@link SalesChannel} is set as the new default selection.
     *
     * @param request the {@link SalesChannelRequest} containing the status and default selection flag to use when setting the default selection
     * @param salesChannel the {@link SalesChannel} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(SalesChannelRequest request, SalesChannel salesChannel) {
        if (request.getStatus().equals(INACTIVE)) {
            salesChannel.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<SalesChannel> currentDefaultSalesChannelOptional = salesChannelRepository.findByDefaultSelectionTrue();
                if (currentDefaultSalesChannelOptional.isPresent()) {
                    SalesChannel currentDefaultSalesChannel = currentDefaultSalesChannelOptional.get();
                    currentDefaultSalesChannel.setDefaultSelection(false);
                    salesChannelRepository.save(currentDefaultSalesChannel);
                }
            }
            salesChannel.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Retrieves detailed information about {@link SalesChannel} by ID
     *
     * @param id ID of {@link SalesChannel}
     * @return {@link SalesChannelResponse}
     * @throws DomainEntityNotFoundException if no {@link SalesChannel} was found with the provided ID.
     */
    public SalesChannelResponse view(Long id) {
        log.debug("Fetching sales channel with ID: {}", id);
        SalesChannel salesChannel = salesChannelRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales channel not found, ID: " + id));
        PortalTagResponse tagResponse=null;
        if(salesChannel.getPortalTagId()!=null){
            tagResponse=new PortalTagResponse( portalTagRepository.findById(salesChannel.getPortalTagId())
                    .orElseThrow(()-> new DomainEntityNotFoundException("portalTagId-Portal tag does not exist!;")));
        }
        return salesChannelMapper.responseFromEntity(salesChannel,tagResponse);
    }

    /**
     * Edit the requested {@link SalesChannel}.
     * If the request asks to save {@link SalesChannel} as a default and a default {@link SalesChannel} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * If the status inside request is {@link NomenclatureItemStatus#INACTIVE}, default selection will be set false,
     * no matter the requested default selection.
     *
     * @param id ID of {@link SalesChannel}
     * @param request {@link SalesChannelRequest}
     * @throws DomainEntityNotFoundException if {@link SalesChannel} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link SalesChannel} is deleted.
     * @return {@link SalesChannelResponse}
     */
    @Transactional
    public SalesChannelResponse edit(Long id, SalesChannelRequest request) {
        log.debug("Editing sales channel: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        SalesChannel salesChannel = salesChannelRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sales channel not found, ID: " + id));

        if (salesChannel.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }



        if (salesChannelRepository.countSalesChannelByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !salesChannel.getName().equals(request.getName().trim())) {
            log.error("Sales channel with such name already exists");
            throw new ClientException("name-Sales channel with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        PortalTagResponse tagResponse=null;
        if(request.getPortalTagId()!=null){
            tagResponse=new PortalTagResponse(portalTagRepository.findByIdAndStatus(request.getPortalTagId(),EntityStatus.ACTIVE)
                    .orElseThrow(()-> new DomainEntityNotFoundException("portalTagId-Portal tag does not exist!;")));
        }
        salesChannel.setPortalTagId(request.getPortalTagId());
        assignDefaultSelectionWhenEditing(request, salesChannel);

        salesChannel.setName(request.getName().trim());
        salesChannel.setStatus(request.getStatus());
        salesChannel.setOffPremisesContracts(request.getOffPremisesContracts());

        return salesChannelMapper.responseFromEntity(salesChannel,tagResponse);
    }

    /**
     * Sets the default selection flag for the given {@link SalesChannel} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link SalesChannel} is set to false,
     * and the given {@link SalesChannel} is set as the new default selection.
     *
     * @param request the {@link SalesChannelRequest} containing the status and default selection flag to use when setting the default selection
     * @param salesChannel the {@link SalesChannel} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(SalesChannelRequest request, SalesChannel salesChannel) {
        if (request.getStatus().equals(INACTIVE)) {
            salesChannel.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!salesChannel.isDefaultSelection()) {
                    Optional<SalesChannel> currentDefaultSalesChannelOptional = salesChannelRepository.findByDefaultSelectionTrue();
                    if (currentDefaultSalesChannelOptional.isPresent()) {
                        SalesChannel currentDefaultSalesChannel = currentDefaultSalesChannelOptional.get();
                        currentDefaultSalesChannel.setDefaultSelection(false);
                        salesChannelRepository.save(currentDefaultSalesChannel);
                    }
                }
            }
            salesChannel.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
