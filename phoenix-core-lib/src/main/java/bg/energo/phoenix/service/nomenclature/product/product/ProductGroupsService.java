package bg.energo.phoenix.service.nomenclature.product.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.product.ProductGroupsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ProductGroupsResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductGroupsRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductGroupsService implements NomenclatureBaseService {
    private final ProductGroupsRepository productGroupsRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PRODUCT_GROUPS;
    }

    /**
     * Adds {@link ProductGroups} at the end with the highest ordering ID.
     * If the request asks to save {@link ProductGroups} as a default and a default {@link ProductGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ProductGroupsRequest}
     * @return {@link ProductGroupsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ProductGroupsResponse add(ProductGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());
        log.debug("Adding Product Groups: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<ProductGroups> productGroupsByName = productGroupsRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (productGroupsByName.size() > 0) {
            log.error("Product Group with presented name already exists");
            throw new ClientException("name-Product Group with presented name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = productGroupsRepository.findLastOrderingId();
        ProductGroups productGroups = new ProductGroups(request);
        productGroups.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, productGroups);
        ProductGroups savedProductGroups = productGroupsRepository.save(productGroups);
        return new ProductGroupsResponse(savedProductGroups);
    }

    /**
     * Edit the requested {@link ProductGroups}.
     * If the request asks to save {@link ProductGroups} as a default and a default {@link ProductGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link ProductGroups}
     * @param request {@link ProductGroupsRequest}
     * @return {@link ProductGroupsResponse}
     * @throws DomainEntityNotFoundException if {@link ProductGroups} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link ProductGroups} is deleted.
     */
    @Transactional
    public ProductGroupsResponse edit(Long id, ProductGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());
        log.debug("Editing Product Group: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ProductGroups productGroups = productGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Product Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        if (!productGroups.getName().equalsIgnoreCase(request.getName())) {
            List<ProductGroups> productGroupsByName = productGroupsRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
            if (productGroupsByName.size() > 0) {
                log.error("Product Group with presented name already exists");
                throw new ClientException("name-Product Group with presented name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (productGroups.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, productGroups);

        productGroups.setName(request.getName());
        productGroups.setNameTransliterated(request.getNameTransliterated());
        productGroups.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            productGroups.setDefaultSelection(false);
        }
        return new ProductGroupsResponse(productGroupsRepository.save(productGroups));
    }

    private void checkCurrentDefaultSelection(ProductGroupsRequest request, ProductGroups productGroups) {
        if (request.getStatus().equals(INACTIVE)) {
            productGroups.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<ProductGroups> currentDefaultProductGroupsOptional = productGroupsRepository.findByDefaultSelectionTrue();
                if (currentDefaultProductGroupsOptional.isPresent()) {
                    ProductGroups defaultProductGroups = currentDefaultProductGroupsOptional.get();
                    defaultProductGroups.setDefaultSelection(false);
                    productGroupsRepository.save(defaultProductGroups);
                }
            }
            productGroups.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Deletes {@link ProductGroups} if the validations are passed.
     *
     * @param id ID of the {@link ProductGroups}
     * @throws DomainEntityNotFoundException if {@link ProductGroups} is not found.
     * @throws OperationNotAllowedException  if the {@link ProductGroups} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ProductGroups} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.PRODUCT_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Product Group with ID: {}", id);
        ProductGroups productGroups = productGroupsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product Group not found"));

        if (productGroups.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        Long activeConnections = productGroupsRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        productGroups.setDefaultSelection(false);
        productGroups.setStatus(DELETED);
        productGroupsRepository.save(productGroups);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return productGroupsRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return productGroupsRepository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link ProductGroupsResponse} by ID
     *
     * @param id ID of {@link ProductGroups}
     * @return {@link ProductGroupsResponse}
     * @throws DomainEntityNotFoundException if no {@link ProductGroups} was found with the provided ID.
     */
    public ProductGroupsResponse view(Long id) {
        log.debug("Fetching Product Group with ID: {}", id);
        ProductGroups productGroups = productGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Product Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        return new ProductGroupsResponse(productGroups);
    }

    /**
     * Filters {@link ProductGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ProductGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ProductGroupsResponse> Page&lt;ProductGroupsResponse&gt;} containing detailed information
     */
    public Page<ProductGroupsResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Product Groups list with request: {}", request.toString());
        Page<ProductGroups> page = productGroupsRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ProductGroupsResponse::new);
    }

    /**
     * Filters {@link ProductGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ProductGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.PRODUCT_GROUPS, permissions = {PermissionEnum.NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Product Groups list with statuses: {}", request);
        return productGroupsRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link ProductGroups} item in the ProductGroups list to a specified position.
     * The method retrieves the {@link ProductGroups} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ProductGroups} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ProductGroups} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.PRODUCT_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in Product Groups to top", request.getId());

        ProductGroups productGroups = productGroupsRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-Product Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ProductGroups> productGroupsList;

        if (productGroups.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = productGroups.getOrderingId();
            productGroupsList = productGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    productGroups.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ProductGroups pg : productGroupsList) {
                pg.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = productGroups.getOrderingId();
            end = request.getOrderingId();
            productGroupsList = productGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    productGroups.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ProductGroups pg : productGroupsList) {
                pg.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        productGroups.setOrderingId(request.getOrderingId());
        productGroupsRepository.save(productGroups);
        productGroupsRepository.saveAll(productGroupsList);
    }

    /**
     * Sorts all {@link ProductGroups} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.PRODUCT_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Product Groups alphabetically");
        List<ProductGroups> productGroups = productGroupsRepository.orderByName();
        long orderingId = 1;

        for (ProductGroups groups : productGroups) {
            groups.setOrderingId(orderingId);
            orderingId++;
        }

        productGroupsRepository.saveAll(productGroups);
    }
}
