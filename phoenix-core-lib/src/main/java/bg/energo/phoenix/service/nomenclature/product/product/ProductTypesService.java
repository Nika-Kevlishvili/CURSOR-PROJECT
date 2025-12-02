package bg.energo.phoenix.service.nomenclature.product.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.product.ProductTypesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ProductTypesResponse;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_TYPE;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTypesService implements NomenclatureBaseService {

    private final ProductTypeRepository productTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PRODUCT_TYPES;
    }

    /**
     * Filters {@link ProductTypes} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ProductTypes}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page< ProductTypesResponse > Page&lt;ProductTypesResponse&gt;} containing detailed information
     */
    public Page<ProductTypesResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ProductTypes list with request: {}", request.toString());
        Page<ProductTypes> page = productTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ProductTypesResponse::new);
    }

    /**
     * Filters {@link ProductTypes} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ProductTypes}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRODUCT_TYPE, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return productTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link ProductTypes} at the end with the highest ordering ID.
     * If the request asks to save {@link ProductTypes} as a default and a default {@link ProductTypes} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * function also checks if request name is unique and if not returns exception
     * @param request {@link ProductTypes}
     * @return {@link ProductTypesResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ProductTypesResponse add(ProductTypesRequest request) {

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED",ILLEGAL_ARGUMENTS_PROVIDED);
        }
        String name = request.getName();
        request.setName(name.trim());
        Integer count = getExistingRecordsCountByName(request.getName());
        if (count > 0) {
            log.error("ProductType Name is not unique");
            throw new ClientException("name-ProductType Name is not unique",ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Long lastSortOrder = productTypeRepository.findLastOrderingId();
        ProductTypes productTypes = new ProductTypes(request);
        productTypes.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(),request.getDefaultSelection(),productTypes);
        ProductTypes productType = productTypeRepository.save(productTypes);
        return new ProductTypesResponse(productType);
    }

    /**
     * Retrieves detailed information about {@link ProductTypes} by ID
     *
     * @param id ID of {@link ProductTypes}
     * @return {@link ProductTypesResponse}
     * @throws DomainEntityNotFoundException if no {@link ProductTypes} was found with the provided ID.
     */
    public ProductTypesResponse view(Long id) {
        log.debug("Fetching ProductTypes with ID: {}", id);
        ProductTypes productTypes = productTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new ProductTypesResponse(productTypes);
    }

    /**
     * Edit the requested {@link ProductTypes}.
     * If the request asks to save {@link ProductTypes} as a default and a default {@link ProductTypes} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link ProductTypes}
     * @param request {@link ProductTypesRequest}
     * @return {@link ProductTypesResponse}
     * @throws DomainEntityNotFoundException if {@link ProductTypes} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link ProductTypes} is deleted.
     */
    @Transactional
    public ProductTypesResponse edit(Long id, ProductTypesRequest request) {
        log.debug("Editing ProductTypes: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        
        String name = request.getName();
        request.setName(name.trim());
        ProductTypes productTypes = productTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));
        if (productTypes.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }
        if(!productTypes.getName().equals(request.getName())){
            if (getExistingRecordsCountByName(request.getName())> 0) {
                    log.error("ProductType Name is not unique");
                    throw new ClientException("name-ProductType Name is not unique",ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        assignDefaultSelection(request.getStatus(),request.getDefaultSelection(),productTypes);
        productTypes.setName(request.getName());
        productTypes.setStatus(request.getStatus());
        return new ProductTypesResponse(productTypeRepository.save(productTypes));
    }
    /**
     * AssignDefaultSelection
     * @param status
     * @param isDefaultSelection
     * @param productTypes
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean isDefaultSelection,ProductTypes productTypes) {
        if (status.equals(INACTIVE)) {
            productTypes.setIsDefault(false);
        } else {
            if (isDefaultSelection) {
                    Optional<ProductTypes> currentDefaultProductTypeOptional = productTypeRepository.findByIsDefaultTrue();
                    if (currentDefaultProductTypeOptional.isPresent()) {
                        ProductTypes currentDefaultProductType = currentDefaultProductTypeOptional.get();
                        currentDefaultProductType.setIsDefault(false);
                        productTypeRepository.save(currentDefaultProductType);
                    }
                productTypes.setIsDefault(true);
            } else {
                productTypes.setIsDefault(false);
            }
        }
    }

    /**
     * <h1>Check Name For Uniqueness</h1>
     * function returns count of name in database, if name is > 0 it means that it's not unique
     *
     * @param name
     * @return Integer count of name
     */
    private Integer getExistingRecordsCountByName(String name) {
        return productTypeRepository.getExistingRecordsCountByName(name.toLowerCase(), List.of(NomenclatureItemStatus.ACTIVE,INACTIVE));
    }

    /**
     * Changes the ordering of a {@link ProductTypes} item in the ProductTypes list to a specified position.
     * The method retrieves the {@link ProductTypes} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ProductTypes} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ProductTypes} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRODUCT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in ProductTypes to top", request.getId());

        ProductTypes productTypes = productTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ProductTypes> productTypesLis;

        if (productTypes.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = productTypes.getOrderingId();
            productTypesLis = productTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    productTypes.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ProductTypes c : productTypesLis) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = productTypes.getOrderingId();
            end = request.getOrderingId();
            productTypesLis = productTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    productTypes.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ProductTypes c : productTypesLis) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        productTypes.setOrderingId(request.getOrderingId());
        productTypeRepository.save(productTypes);
        productTypeRepository.saveAll(productTypesLis);
    }

    /**
     * Sorts all {@link ProductTypes} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRODUCT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the productTypesList alphabetically");
        List<ProductTypes> productTypesList = productTypeRepository.orderByName();
        long orderingId = 1;

        for (ProductTypes c : productTypesList) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        productTypeRepository.saveAll(productTypesList);
    }

    /**
     * Deletes {@link ProductTypes} if the validations are passed.
     *
     * @param id ID of the {@link ProductTypes}
     * @throws DomainEntityNotFoundException if {@link ProductTypes} is not found.
     * @throws OperationNotAllowedException  if the {@link ProductTypes} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ProductTypes} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRODUCT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing productTypes with ID: {}", id);
        ProductTypes productTypes = productTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long activeConnections = productTypeRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (productTypes.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }
        // TODO:Check if there is no connected object to this nomenclature item in system
        productTypes.setStatus(DELETED);
        productTypeRepository.save(productTypes);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return productTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return productTypeRepository.findByIdIn(ids);
    }
}
