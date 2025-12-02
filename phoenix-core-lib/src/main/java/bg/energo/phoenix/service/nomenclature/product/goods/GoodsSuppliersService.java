package bg.energo.phoenix.service.nomenclature.product.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.goods.GoodsSuppliersRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsSuppliersResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsSuppliersRepository;
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
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSuppliersService implements NomenclatureBaseService {
    private final GoodsSuppliersRepository goodsSuppliersRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.GOODS_SUPPLIERS;
    }

    /**
     * Adds {@link GoodsSuppliers} at the end with the highest ordering ID.
     * If the request asks to save {@link GoodsSuppliers} as a default and a default {@link GoodsSuppliers} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link GoodsSuppliersRequest}
     * @return {@link GoodsSuppliersResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public GoodsSuppliersResponse add(GoodsSuppliersRequest request) {
        request.setName(request.getName().trim());
        log.debug("Adding Goods Suppliers: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<GoodsSuppliers> goodsSuppliersWithName = goodsSuppliersRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (goodsSuppliersWithName.size() > 0) {
            log.error("Cannot add item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot add item with name [%s], goods suppliers with same name already exists", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = goodsSuppliersRepository.findLastOrderingId();
        GoodsSuppliers goodsSupplier = new GoodsSuppliers(request);
        goodsSupplier.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, goodsSupplier);
        GoodsSuppliers goodsSupplierEntity = goodsSuppliersRepository.save(goodsSupplier);
        return new GoodsSuppliersResponse(goodsSupplierEntity);
    }

    /**
     * Edit the requested {@link GoodsSuppliers}.
     * If the request asks to save {@link GoodsSuppliers} as a default and a default {@link GoodsSuppliers} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link GoodsSuppliers}
     * @param request {@link GoodsSuppliersRequest}
     * @return {@link GoodsSuppliersResponse}
     * @throws DomainEntityNotFoundException if {@link GoodsSuppliers} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link GoodsSuppliers} is deleted.
     */
    @Transactional
    public GoodsSuppliersResponse edit(Long id, GoodsSuppliersRequest request) {
        request.setName(request.getName().trim());
        log.debug("Editing GoodsSupplier: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GoodsSuppliers goodsSupplier = goodsSuppliersRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Goods Supplier with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        List<GoodsSuppliers> goodsSuppliersWithName = goodsSuppliersRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (goodsSuppliersWithName.size() > 0 && goodsSupplier.getId() != id) {
            log.error("Cannot edit item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot edit item with name [%s], goods suppliers with same name already exists", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (goodsSupplier.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, goodsSupplier);

        goodsSupplier.setName(request.getName());
        goodsSupplier.setIdentifier(request.getIdentifier());
        goodsSupplier.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            goodsSupplier.setDefaultSelection(false);
        }
        return new GoodsSuppliersResponse(goodsSuppliersRepository.save(goodsSupplier));
    }

    private void checkCurrentDefaultSelection(GoodsSuppliersRequest request, GoodsSuppliers goodsSupplier) {
        if (request.getStatus().equals(INACTIVE)) {
            goodsSupplier.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<GoodsSuppliers> currentDefaultGoodsSuppliersOptional = goodsSuppliersRepository.findByDefaultSelectionTrue();
                if (currentDefaultGoodsSuppliersOptional.isPresent()) {
                    GoodsSuppliers defaultGoodsSupplier = currentDefaultGoodsSuppliersOptional.get();
                    defaultGoodsSupplier.setDefaultSelection(false);
                    goodsSuppliersRepository.save(defaultGoodsSupplier);
                }
            }
            goodsSupplier.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Deletes {@link GoodsSuppliers} if the validations are passed.
     *
     * @param id ID of the {@link GoodsSuppliers}
     * @throws DomainEntityNotFoundException if {@link GoodsSuppliers} is not found.
     * @throws OperationNotAllowedException  if the {@link GoodsSuppliers} is already deleted.
     * @throws OperationNotAllowedException  if the {@link GoodsSuppliers} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_SUPPLIERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Goods Supplier with ID: {}", id);
        GoodsSuppliers goodsSupplier = goodsSuppliersRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Goods Supplier not found"));

        if (goodsSupplier.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        Long activeConnections = goodsSuppliersRepository.activeConnectionCount(
                id,
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        goodsSupplier.setDefaultSelection(false);
        goodsSupplier.setStatus(DELETED);
        goodsSuppliersRepository.save(goodsSupplier);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return goodsSuppliersRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return goodsSuppliersRepository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link GoodsSuppliersResponse} by ID
     *
     * @param id ID of {@link GoodsSuppliers}
     * @return {@link GoodsSuppliersResponse}
     * @throws DomainEntityNotFoundException if no {@link GoodsSuppliers} was found with the provided ID.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_SUPPLIERS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public GoodsSuppliersResponse view(Long id) {
        log.debug("Fetching Goods Supplier with ID: {}", id);
        GoodsSuppliers goodsSupplier = goodsSuppliersRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Goods Supplier with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        return new GoodsSuppliersResponse(goodsSupplier);
    }

    /**
     * Filters {@link GoodsSuppliers} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsSuppliers}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<GoodsSuppliersResponse> Page&lt;GoodsSuppliersResponse&gt;} containing detailed information
     */

    public Page<GoodsSuppliersResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Goods Suppliers list with request: {}", request.toString());
        Page<GoodsSuppliers> page = goodsSuppliersRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(GoodsSuppliersResponse::new);
    }

    /**
     * Filters {@link GoodsSuppliers} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsSuppliers}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_SUPPLIERS, permissions = {PermissionEnum.NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Goods Suppliers list with statuses: {}", request);
        return goodsSuppliersRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link GoodsSuppliers} item in the GoodsSuppliers list to a specified position.
     * The method retrieves the {@link GoodsSuppliers} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link GoodsSuppliers} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link GoodsSuppliers} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_SUPPLIERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in GoodsSupplier to top", request.getId());

        GoodsSuppliers goodsSupplier = goodsSuppliersRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-Goods Supplier with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<GoodsSuppliers> goodsSuppliers;

        if (goodsSupplier.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = goodsSupplier.getOrderingId();
            goodsSuppliers = goodsSuppliersRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsSupplier.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GoodsSuppliers gs : goodsSuppliers) {
                gs.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = goodsSupplier.getOrderingId();
            end = request.getOrderingId();
            goodsSuppliers = goodsSuppliersRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsSupplier.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (GoodsSuppliers gs : goodsSuppliers) {
                gs.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        goodsSupplier.setOrderingId(request.getOrderingId());
        goodsSuppliersRepository.save(goodsSupplier);
        goodsSuppliersRepository.saveAll(goodsSuppliers);
    }

    /**
     * Sorts all {@link GoodsSuppliers} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.GOODS_SUPPLIERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the GoodsSuppliers alphabetically");
        List<GoodsSuppliers> goodsSuppliers = goodsSuppliersRepository.orderByName();
        long orderingId = 1;

        for (GoodsSuppliers c : goodsSuppliers) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        goodsSuppliersRepository.saveAll(goodsSuppliers);
    }
}
