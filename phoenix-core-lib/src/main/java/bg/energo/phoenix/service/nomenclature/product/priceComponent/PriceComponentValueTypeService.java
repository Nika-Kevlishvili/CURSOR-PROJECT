package bg.energo.phoenix.service.nomenclature.product.priceComponent;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentValueType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.PriceComponentValueTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentValueTypeResponse;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.PriceComponentValueTypeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.PC_VALUE_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@RequiredArgsConstructor
@Service
@Slf4j
public class PriceComponentValueTypeService implements NomenclatureBaseService {

    private final PriceComponentValueTypeRepository valueTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PC_VALUE_TYPE;
    }
    /**
     * Filters {@link PriceComponentValueType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link PriceComponentValueType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_VALUE_TYPES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Price component value type list with statuses: {}", request);
        return valueTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "orderingId"))
                );
    }
    /**
     * Changes the ordering of a {@link PriceComponentValueType} item in the Price component value type list to a specified position.
     * The method retrieves the {@link PriceComponentValueType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link PriceComponentValueType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link PriceComponentValueType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_VALUE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        PriceComponentValueType valueType = valueTypeRepository
                .findByIdAndStatuses(request.getId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price component value type not found"));

        Long start;
        Long end;
        List<PriceComponentValueType> valueTypes;

        if (valueType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = valueType.getOrderingId();
            valueTypes = valueTypeRepository.findInOrderingIdRange(start, end, valueType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (PriceComponentValueType b : valueTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = valueType.getOrderingId();
            end = request.getOrderingId();
            valueTypes = valueTypeRepository.findInOrderingIdRange(start, end, valueType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (PriceComponentValueType b : valueTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        valueType.setOrderingId(request.getOrderingId());
        valueTypes.add(valueType);
        valueTypeRepository.saveAll(valueTypes);
    }
    /**
     * Sorts all {@link PriceComponentValueType} alphabetically.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_VALUE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Price component value types alphabetically");
        List<PriceComponentValueType> valueTypes = valueTypeRepository.orderByName();
        long orderingId = 1;

        for (PriceComponentValueType b : valueTypes) {
            b.setOrderingId(orderingId);
            orderingId++;
        }
        valueTypeRepository.saveAll(valueTypes);
    }
    /**
     * Deletes {@link PriceComponentValueType}
     *
     * @param id ID of the {@link PriceComponentValueType}
     * @throws DomainEntityNotFoundException if {@link PriceComponentValueType} is not found.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_VALUE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Price component value type  with ID: {}", id);
        PriceComponentValueType elType = valueTypeRepository
                .findByIdAndStatuses(id, List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price component value type not found"));

        Long activeConnections = valueTypeRepository.activeConnectionCount(
                id,
                List.of(PriceComponentStatus.ACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        elType.setStatus(DELETED);
        valueTypeRepository.save(elType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return valueTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return valueTypeRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link PriceComponentValueType} at the end with the highest ordering ID.
     * {@link PriceComponentValueType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param request {@link PriceComponentValueTypeRequest}
     * @return {@link PriceComponentValueTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public PriceComponentValueTypeResponse add(PriceComponentValueTypeRequest request) {
        log.debug("Adding Price component value type: {}", request.toString());


        Optional<PriceComponentValueType> byNameAndStatuses = validateAndFetchWithName(request);
        byNameAndStatuses
                .ifPresent(elWIthName -> {
                    String message = String.format("name-Price component value type with the name %s already exists", elWIthName.getName());
                    log.debug(message);
                    throw new ClientException(message, ErrorCode.CONFLICT);
                });
        Long lastSortOrder = valueTypeRepository.findLastOrderingId();
        PriceComponentValueType valueType = new PriceComponentValueType(request);
        valueType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        updateDefaultForAdd(valueType);
        PriceComponentValueType valueTypeEntity = valueTypeRepository.save(valueType);
        return new PriceComponentValueTypeResponse(valueTypeEntity);
    }

    private void updateDefaultForAdd(PriceComponentValueType valueType) {
        if (valueType.isDefaultSelection()) {
            Optional<PriceComponentValueType> currentDefaultOptional = valueTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                PriceComponentValueType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                valueTypeRepository.save(currentDefault);
            }
        }
    }


    /**
     * Edit the requested {@link PriceComponentValueType}.
     * {@link PriceComponentValueType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param id      ID of {@link PriceComponentValueType}
     * @param request {@link PriceComponentValueTypeRequest}
     * @return {@link PriceComponentValueTypeResponse}
     * @throws DomainEntityNotFoundException if {@link PriceComponentValueType} is not found with Active or Inactive status.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request has status DELETED.
     */
    @Transactional
    public PriceComponentValueTypeResponse edit(Long id, PriceComponentValueTypeRequest request) {
        log.debug("Adding Price component value type: {}", request.toString());

        Optional<PriceComponentValueType> byNameAndStatuses = validateAndFetchWithName(request);


        PriceComponentValueType elType = valueTypeRepository
                .findByIdAndStatuses(id,List.of(ACTIVE,INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service not found"));

        byNameAndStatuses
                .ifPresent(elWIthName -> {
                    if (!elWIthName.getId().equals(elType.getId())){
                        String message = String.format("name-Price component value type with the name %s already exists", elWIthName.getName());
                        log.error(message);
                        throw new ClientException(message, ErrorCode.CONFLICT);
                    }
                });

        updateDefaultForedit(request, elType);

        elType.setName(request.getName().trim());
        elType.setDefaultSelection(request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection());
        elType.setStatus(request.getStatus());
        return new PriceComponentValueTypeResponse(valueTypeRepository.save(elType));
    }

    private void updateDefaultForedit(PriceComponentValueTypeRequest request, PriceComponentValueType elType) {
        if ( request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection()&& !elType.isDefaultSelection()) {
            Optional<PriceComponentValueType> currentDefaultOptional = valueTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                PriceComponentValueType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                valueTypeRepository.save(currentDefault);
            }
            elType.setDefaultSelection(true);
        }
    }


    /**
     * Filters {@link PriceComponentValueType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link PriceComponentValueType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<PriceComponentValueTypeResponse> Page&lt;PriceComponentValueTypeResponse&gt;} containing detailed information
     */

    public Page<PriceComponentValueTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Price component value types list with : {}" + request.toString());
        Page<PriceComponentValueType> page = valueTypeRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        return page.map(PriceComponentValueTypeResponse::new);
    }

    /**
     * Retrieves detailed information about {@link PriceComponentValueType} by ID
     *
     * @param id ID of {@link PriceComponentValueType}
     * @return {@link PriceComponentValueTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link PriceComponentValueType} was found with the provided ID.
     */
    public PriceComponentValueTypeResponse view(Long id) {
        log.debug("Fetching Price component value type with ID: {}", id);
        PriceComponentValueType elType = valueTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price component value type not found"));
        return new PriceComponentValueTypeResponse(elType);
    }
    /**
     * Validates request checks if:
     * 1. request status is not deleted
     * 2. Price component value type with name does not exist
     *
     * @param request to validate {@link PriceComponentValueTypeRequest}
     * @throws ClientException with ErrorCode ILLEGAL_ARGUMENTS_PROVIDED when status in request is Deleted
     * @throws ClientException with ErrorCode CONFLICT when another {@link PriceComponentValueType} exists with same name
     */
    private Optional<PriceComponentValueType> validateAndFetchWithName(PriceComponentValueTypeRequest request) {
        if (request.getStatus().equals(DELETED)) {
            String msg = "status-Cannot add item with status DELETED";
            log.error(msg);
            throw new ClientException(msg,ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return valueTypeRepository.findByNameAndStatuses(request.getName().trim(), List.of(ACTIVE, INACTIVE));

    }
}
