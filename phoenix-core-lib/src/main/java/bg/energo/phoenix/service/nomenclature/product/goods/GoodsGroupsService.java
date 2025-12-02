package bg.energo.phoenix.service.nomenclature.product.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.goods.GoodsGroupsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsGroupsResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsGroupsRepository;
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
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsGroupsService implements NomenclatureBaseService {
    private final GoodsGroupsRepository goodsGroupsRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.GOODS_GROUPS;
    }

    /**
     * Adds {@link GoodsGroups} at the end with the highest ordering ID.
     * If the request asks to save {@link GoodsGroups} as a default and a default {@link GoodsGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link GoodsGroupsRequest}
     * @return {@link GoodsGroupsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public GoodsGroupsResponse add(GoodsGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());

        log.debug("Adding Goods Groups: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<GoodsGroups> goodsGroupsWithName = goodsGroupsRepository.findByNameAndStatuses(request.getName(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (goodsGroupsWithName.size() > 0) {
            log.error("Cannot add item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot add item with name [%s], goods groups with same name already exists", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = goodsGroupsRepository.findLastOrderingId();
        GoodsGroups goodsGroups = new GoodsGroups(request);
        goodsGroups.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, goodsGroups);
        GoodsGroups goodsGroupsEntity = goodsGroupsRepository.save(goodsGroups);
        return new GoodsGroupsResponse(goodsGroupsEntity);
    }

    /**
     * Edit the requested {@link GoodsGroups}.
     * If the request asks to save {@link GoodsGroups} as a default and a default {@link GoodsGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link GoodsGroups}
     * @param request {@link GoodsGroupsRequest}
     * @return {@link GoodsGroupsResponse}
     * @throws DomainEntityNotFoundException if {@link GoodsGroups} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link GoodsGroups} is deleted.
     */
    @Transactional
    public GoodsGroupsResponse edit(Long id, GoodsGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());
        log.debug("Editing Goods Group: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GoodsGroups goodsGroup = goodsGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Goods Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        List<GoodsGroups> goodsGroupsWithName = goodsGroupsRepository.findByNameAndStatuses(request.getName(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (goodsGroupsWithName.size() > 0 && goodsGroup.getId() != id) {
            log.error("Cannot edit item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot edit item with name [%s], goods groups with same name already exists", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (goodsGroup.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, goodsGroup);

        goodsGroup.setName(request.getName());
        goodsGroup.setNameTransliterated(request.getNameTransliterated());
        goodsGroup.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            goodsGroup.setDefaultSelection(false);
        }
        return new GoodsGroupsResponse(goodsGroupsRepository.save(goodsGroup));
    }

    private void checkCurrentDefaultSelection(GoodsGroupsRequest request, GoodsGroups goodsGroup) {
        if (request.getStatus().equals(INACTIVE)) {
            goodsGroup.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<GoodsGroups> currentDefaultGoodsGroupsOptional = goodsGroupsRepository.findByDefaultSelectionTrue();
                if (currentDefaultGoodsGroupsOptional.isPresent()) {
                    GoodsGroups defaultGoodsGroup = currentDefaultGoodsGroupsOptional.get();
                    defaultGoodsGroup.setDefaultSelection(false);
                    goodsGroupsRepository.save(defaultGoodsGroup);
                }
            }
            goodsGroup.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Deletes {@link GoodsGroups} if the validations are passed.
     *
     * @param id ID of the {@link GoodsGroups}
     * @throws DomainEntityNotFoundException if {@link GoodsGroups} is not found.
     * @throws OperationNotAllowedException  if the {@link GoodsGroups} is already deleted.
     * @throws OperationNotAllowedException  if the {@link GoodsGroups} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Goods Group with ID: {}", id);
        GoodsGroups goodsGroup = goodsGroupsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Goods Group not found"));

        if (goodsGroup.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        Long activeConnections = goodsGroupsRepository.activeConnectionCount(
                id,
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        goodsGroup.setDefaultSelection(false);
        goodsGroup.setStatus(DELETED);
        goodsGroupsRepository.save(goodsGroup);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return goodsGroupsRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return goodsGroupsRepository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link GoodsGroupsResponse} by ID
     *
     * @param id ID of {@link GoodsGroups}
     * @return {@link GoodsGroupsResponse}
     * @throws DomainEntityNotFoundException if no {@link GoodsGroups} was found with the provided ID.
     */
    public GoodsGroupsResponse view(Long id) {
        log.debug("Fetching Goods Group with ID: {}", id);
        GoodsGroups goodsGroup = goodsGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Goods Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        return new GoodsGroupsResponse(goodsGroup);
    }

    /**
     * Filters {@link GoodsGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<GoodsGroupsResponse> Page&lt;GoodsGroupsResponse&gt;} containing detailed information
     */
    public Page<GoodsGroupsResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Goods Groups list with request: {}", request.toString());
        Page<GoodsGroups> page = goodsGroupsRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(GoodsGroupsResponse::new);
    }

    /**
     * Filters {@link GoodsGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_GROUPS, permissions = {PermissionEnum.NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Goods Groups list with statuses: {}", request);
        return goodsGroupsRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link GoodsGroups} item in the GoodsGroups list to a specified position.
     * The method retrieves the {@link GoodsGroups} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link GoodsGroups} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link GoodsGroups} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in Goods Groups to top", request.getId());

        GoodsGroups goodsGroup = goodsGroupsRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-Goods Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<GoodsGroups> goodsGroups;

        if (goodsGroup.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = goodsGroup.getOrderingId();
            goodsGroups = goodsGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsGroup.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GoodsGroups gg : goodsGroups) {
                gg.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = goodsGroup.getOrderingId();
            end = request.getOrderingId();
            goodsGroups = goodsGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsGroup.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (GoodsGroups gg : goodsGroups) {
                gg.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        goodsGroup.setOrderingId(request.getOrderingId());
        goodsGroupsRepository.save(goodsGroup);
        goodsGroupsRepository.saveAll(goodsGroups);
    }

    /**
     * Sorts all {@link GoodsGroups} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Goods Groups alphabetically");
        List<GoodsGroups> goodsGroups = goodsGroupsRepository.orderByName();
        long orderingId = 1;

        for (GoodsGroups groups : goodsGroups) {
            groups.setOrderingId(orderingId);
            orderingId++;
        }

        goodsGroupsRepository.saveAll(goodsGroups);
    }
}
