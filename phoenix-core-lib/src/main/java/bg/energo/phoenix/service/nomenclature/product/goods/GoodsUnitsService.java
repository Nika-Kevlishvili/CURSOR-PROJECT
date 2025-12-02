package bg.energo.phoenix.service.nomenclature.product.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.goods.GoodsUnitsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsResponse;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.GOODS_UNITS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsUnitsService implements NomenclatureBaseService {

    private final GoodsUnitsRepository goodsUnitsRepository;


    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.GOODS_UNITS;
    }

    /**
     * Filters {@link GoodsUnits} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsUnits}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<GoodsUnitsResponse> Page&lt;GoodsUnitsResponse&gt;} containing detailed information
     */
    public Page<GoodsUnitsResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering GoodsUnits list with request: {}", request.toString());
        Page<GoodsUnits> page = goodsUnitsRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(GoodsUnitsResponse::new);
    }

    /**
     * Filters {@link GoodsUnits} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link GoodsUnits}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GOODS_UNITS, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return goodsUnitsRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link GoodsUnits} at the end with the highest ordering ID.
     * If the request asks to save {@link GoodsUnits} as a default and a default {@link GoodsUnits} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * function also checks if request name is unique in database and returns exception if it's not unique
     *
     * @param request {@link GoodsUnits}
     * @return {@link GoodsUnitsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public GoodsUnitsResponse add(GoodsUnitsRequest request) {

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED",ILLEGAL_ARGUMENTS_PROVIDED);
        }
        String name = request.getName();
        request.setName(name.trim());
        Integer count = getExistingRecordsCountByName(request.getName());
        if (count > 0) {
            log.error("GoodsUnits Name is not unique");
            throw new ClientException("name-GoodsUnits Name is not unique",ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Long lastSortOrder = goodsUnitsRepository.findLastOrderingId();
        GoodsUnits goodsUnits = new GoodsUnits(request);
        goodsUnits.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), goodsUnits);
        GoodsUnits goodsUnit = goodsUnitsRepository.save(goodsUnits);
        return new GoodsUnitsResponse(goodsUnit);
    }

    /**
     * Retrieves detailed information about {@link GoodsUnits} by ID
     *
     * @param id ID of {@link GoodsUnits}
     * @return {@link GoodsUnitsResponse}
     * @throws DomainEntityNotFoundException if no {@link GoodsUnits} was found with the provided ID.
     */
    public GoodsUnitsResponse view(Long id) {
        log.debug("Fetching GoodsUnits with ID: {}", id);
        GoodsUnits goodsUnits = goodsUnitsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new GoodsUnitsResponse(goodsUnits);
    }

    /**
     * Edit the requested {@link GoodsUnits}.
     * If the request asks to save {@link GoodsUnits} as a default and a default {@link GoodsUnits} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link GoodsUnits}
     * @param request {@link GoodsUnitsRequest}
     * @return {@link GoodsUnitsResponse}
     * @throws DomainEntityNotFoundException if {@link GoodsUnits} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link GoodsUnits} is deleted.
     */
    @Transactional
    public GoodsUnitsResponse edit(Long id, GoodsUnitsRequest request) {
        log.debug("Editing GoodsUnits: {}, with ID: {}", request.toString(), id);
        String name = request.getName();
        request.setName(name.trim());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GoodsUnits goodsUnits = goodsUnitsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));
        if (goodsUnits.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!goodsUnits.getName().equals(request.getName())) {
            if (getExistingRecordsCountByName(request.getName()) > 0) {
                    log.error("GoodsUnits Name is not unique");
                    throw new ClientException("name-GoodsUnits Name is not unique",ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }


        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), goodsUnits);
        goodsUnits.setName(request.getName());
        goodsUnits.setStatus(request.getStatus());
        return new GoodsUnitsResponse(goodsUnitsRepository.save(goodsUnits));
    }

    /**
     * AssignDefaultSelection
     *
     * @param status
     * @param isDefaultSelection
     * @param goodsUnits
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean isDefaultSelection, GoodsUnits goodsUnits) {
        if (status.equals(INACTIVE)) {
            goodsUnits.setIsDefault(false);
        } else {
            if (isDefaultSelection) {
                    Optional<GoodsUnits> currentDefaultGoodsUnitsOptional = goodsUnitsRepository.findByIsDefaultTrue();
                    if (currentDefaultGoodsUnitsOptional.isPresent()) {
                        GoodsUnits currentDefaultGoodsUnit = currentDefaultGoodsUnitsOptional.get();
                        currentDefaultGoodsUnit.setIsDefault(false);
                        goodsUnitsRepository.save(currentDefaultGoodsUnit);
                    }
                goodsUnits.setIsDefault(true);
            } else {
                goodsUnits.setIsDefault(false);
            }
        }
    }

    /**
     * <h1>Check Name For Uniqueness</h1>
     * function returns count of name in database
     *
     * @param name
     * @return Integer count of name
     */
    private Integer getExistingRecordsCountByName(String name) {
        return goodsUnitsRepository.getExistingRecordsCountByName(name.toLowerCase(), List.of(NomenclatureItemStatus.ACTIVE,INACTIVE));
    }

    /**
     * Changes the ordering of a {@link GoodsUnits} item in the GoodsUnits list to a specified position.
     * The method retrieves the {@link GoodsUnits} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link GoodsUnits} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link GoodsUnits} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GOODS_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in GoodsUnits to top", request.getId());

        GoodsUnits goodsUnits = goodsUnitsRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<GoodsUnits> goodsUnitsList;

        if (goodsUnits.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = goodsUnits.getOrderingId();
            goodsUnitsList = goodsUnitsRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsUnits.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GoodsUnits c : goodsUnitsList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = goodsUnits.getOrderingId();
            end = request.getOrderingId();
            goodsUnitsList = goodsUnitsRepository.findInOrderingIdRange(
                    start,
                    end,
                    goodsUnits.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (GoodsUnits c : goodsUnitsList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        goodsUnits.setOrderingId(request.getOrderingId());
        goodsUnitsRepository.save(goodsUnits);
        goodsUnitsRepository.saveAll(goodsUnitsList);
    }

    /**
     * Sorts all {@link GoodsUnits} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GOODS_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the goodsUnitsList alphabetically");
        List<GoodsUnits> goodsUnitsList = goodsUnitsRepository.orderByName();
        long orderingId = 1;

        for (GoodsUnits c : goodsUnitsList) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        goodsUnitsRepository.saveAll(goodsUnitsList);
    }

    /**
     * Deletes {@link GoodsUnits} if the validations are passed.
     *
     * @param id ID of the {@link GoodsUnits}
     * @throws DomainEntityNotFoundException if {@link GoodsUnits} is not found.
     * @throws OperationNotAllowedException  if the {@link GoodsUnits} is already deleted.
     * @throws OperationNotAllowedException  if the {@link GoodsUnits} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = GOODS_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing goodsUnits with ID: {}", id);
        GoodsUnits goodsUnits = goodsUnitsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (goodsUnits.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        Long activeConnections = goodsUnitsRepository.activeConnectionCount(
                id,
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        goodsUnits.setStatus(DELETED);
        goodsUnitsRepository.save(goodsUnits);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return goodsUnitsRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return goodsUnitsRepository.findByIdIn(ids);
    }
}
