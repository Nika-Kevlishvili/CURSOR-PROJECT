package bg.energo.phoenix.service.nomenclature.product;


import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.ElectricityPriceType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.ElectricityPriceTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ElectricityPriceTypeResponse;
import bg.energo.phoenix.repository.nomenclature.product.ElectricityPriceTypeRepository;
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

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRICE_TYPES_ELECTRICITY;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElectricityPriceTypeService implements NomenclatureBaseService {

    private final ElectricityPriceTypeRepository typeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ELECTRICITY_PRICE_TYPE;
    }
    /**
     * Filters {@link ElectricityPriceType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ElectricityPriceType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRICE_TYPES_ELECTRICITY, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Electricity type list with statuses: {}", request);
        return typeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "orderingId"))
                );
    }
    /**
     * Changes the ordering of a {@link ElectricityPriceType} item in the Electricity price type list to a specified position.
     * The method retrieves the {@link ElectricityPriceType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ElectricityPriceType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ElectricityPriceType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRICE_TYPES_ELECTRICITY, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        ElectricityPriceType elType = typeRepository
                .findByIdAndStatuses(request.getId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Electricity Price type not found"));

        Long start;
        Long end;
        List<ElectricityPriceType> elTypes;

        if (elType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = elType.getOrderingId();
            elTypes = typeRepository.findInOrderingIdRange(start, end, elType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (ElectricityPriceType b : elTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = elType.getOrderingId();
            end = request.getOrderingId();
            elTypes = typeRepository.findInOrderingIdRange(start, end, elType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ElectricityPriceType b : elTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        elType.setOrderingId(request.getOrderingId());
        elTypes.add(elType);
        typeRepository.saveAll(elTypes);
    }
    /**
     * Sorts all {@link ElectricityPriceType} alphabetically.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRICE_TYPES_ELECTRICITY, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Electricity Price types alphabetically");
        List<ElectricityPriceType> elTypes = typeRepository.orderByName();
        long orderingId = 1;

        for (ElectricityPriceType b : elTypes) {
            b.setOrderingId(orderingId);
            orderingId++;
        }
        typeRepository.saveAll(elTypes);
    }
    /**
     * Deletes {@link ElectricityPriceType} 
     *
     * @param id ID of the {@link ElectricityPriceType}
     * @throws DomainEntityNotFoundException if {@link ElectricityPriceType} is not found.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PRICE_TYPES_ELECTRICITY, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Electricity Price type  with ID: {}", id);
        ElectricityPriceType elType = typeRepository
                .findByIdAndStatuses(id, List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Electricity Price type not found"));

        Long activeConnections = typeRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        elType.setStatus(DELETED);
        typeRepository.save(elType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return typeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return typeRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link ElectricityPriceType} at the end with the highest ordering ID.
     * {@link ElectricityPriceType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param request {@link ElectricityPriceTypeRequest}
     * @return {@link ElectricityPriceTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ElectricityPriceTypeResponse add(ElectricityPriceTypeRequest request) {
        log.debug("Adding Electricity price type: {}", request.toString());

        validateRequest(request);
        typeRepository.findByNameAndStatuses(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                .ifPresent(elWIthName -> {
                    String message = String.format("name-Electricity price type with the name %s already exists", elWIthName.getName());
                    log.error(message);
                    throw new ClientException(message, ErrorCode.CONFLICT);
                });
        Long lastSortOrder = typeRepository.findLastOrderingId();
        ElectricityPriceType priceType = new ElectricityPriceType(request);

        priceType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);

        if (priceType.isDefaultSelection()) {
            Optional<ElectricityPriceType> currentDefaultOptional = typeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                ElectricityPriceType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                typeRepository.save(currentDefault);
            }
        }

        ElectricityPriceType elTypeEntity = typeRepository.save(priceType);
        return new ElectricityPriceTypeResponse(elTypeEntity);
    }


    /**
     * Edit the requested {@link ElectricityPriceType}.
     * {@link ElectricityPriceType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param id      ID of {@link ElectricityPriceType}
     * @param request {@link ElectricityPriceTypeRequest}
     * @return {@link ElectricityPriceTypeResponse}
     * @throws DomainEntityNotFoundException if {@link ElectricityPriceType} is not found with Active or Inactive status.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request has status DELETED.
     */
    @Transactional
    public ElectricityPriceTypeResponse edit(Long id, ElectricityPriceTypeRequest request) {
        log.debug("Adding Electricity Price type: {}", request.toString());

        validateRequest(request);


        ElectricityPriceType elType = typeRepository
                .findByIdAndStatuses(id,List.of(ACTIVE,INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Electricity not found"));

        typeRepository.findByNameAndStatuses(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                .ifPresent(elWIthName -> {
                    if (!elWIthName.getId().equals(elType.getId())){
                        String message = String.format("name-Electricity price type with the name %s already exists", elWIthName.getName());
                        log.error(message);
                        throw new ClientException(message, ErrorCode.CONFLICT);
                    }
                });

        elType.setName(request.getName().trim());

        if (request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection()&& !elType.isDefaultSelection()) {

            Optional<ElectricityPriceType> currentDefaultOptional = typeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                ElectricityPriceType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                typeRepository.save(currentDefault);
            }
            elType.setDefaultSelection(true);
        }

        elType.setDefaultSelection(request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection());
        elType.setStatus(request.getStatus());
        return new ElectricityPriceTypeResponse(typeRepository.save(elType));
    }

    /**
     * Filters {@link ElectricityPriceType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ElectricityPriceType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ElectricityPriceTypeResponse> Page&lt;ElectricityPriceTypeResponse&gt;} containing detailed information
     */

    public Page<ElectricityPriceTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Electricity Price types list with : {}" + request.toString());
        Page<ElectricityPriceType> page = typeRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize(),Sort.by(Sort.Direction.ASC,"orderingId"))
        );
        return page.map(ElectricityPriceTypeResponse::new);
    }

    /**
     * Retrieves detailed information about {@link ElectricityPriceType} by ID
     *
     * @param id ID of {@link ElectricityPriceType}
     * @return {@link ElectricityPriceTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link ElectricityPriceType} was found with the provided ID.
     */
    public ElectricityPriceTypeResponse view(Long id) {
        log.debug("Fetching Electricity Price type with ID: {}", id);
        ElectricityPriceType elType = typeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Electricity Price type not found"));
        return new ElectricityPriceTypeResponse(elType);
    }
    /**
     * Validates request checks if:
     * 1. request status is not deleted
     * 2. Electricity with name does not exists
     *
     * @param request to validate {@link ElectricityPriceTypeRequest}
     * @throws ClientException with ErrorCode ILLEGAL_ARGUMENTS_PROVIDED when status in request is Deleted
     * @throws ClientException with ErrorCode CONFLICT when another {@link ElectricityPriceType} exists with same name
     */
    private void validateRequest(ElectricityPriceTypeRequest request) {
        if (request.getStatus().equals(DELETED)) {
            String msg = "status-Cannot add item with status DELETED";
            log.error(msg);
            throw new ClientException(msg,ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

    }
}
