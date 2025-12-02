package bg.energo.phoenix.service.nomenclature.product.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.service.ServiceUnitRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ServiceUnitResponse;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceUnitRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.service.nomenclature.mapper.ServiceUnitMapper;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICE_UNITS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceUnitService implements NomenclatureBaseService {

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitMapper serviceUnitMapper;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SERVICE_UNITS;
    }

    /**
     * Filters {@link ServiceUnit} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceUnit#name}.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_UNITS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    @Override
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering service units with statuses: {}", request);
        Page<ServiceUnit> serviceUnits = serviceUnitRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
        return serviceUnits.map(serviceUnitMapper::nomenclatureResponseFromEntity);
    }

    /**
     * Changes the ordering of a {@link ServiceUnit} item in the list to a specified position.
     * The method retrieves the {@link ServiceUnit} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ServiceUnit} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ServiceUnit} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in service units", request.getId());

        ServiceUnit serviceUnit = serviceUnitRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service unit not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<ServiceUnit> serviceUnits;

        if (serviceUnit.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = serviceUnit.getOrderingId();
            serviceUnits = serviceUnitRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            serviceUnit.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ServiceUnit su : serviceUnits) {
                su.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = serviceUnit.getOrderingId();
            end = request.getOrderingId();
            serviceUnits = serviceUnitRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            serviceUnit.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ServiceUnit su : serviceUnits) {
                su.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        serviceUnit.setOrderingId(request.getOrderingId());
        serviceUnits.add(serviceUnit);
        serviceUnitRepository.saveAll(serviceUnits);
    }

    /**
     * Sorts all {@link ServiceUnit} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the service units alphabetically");

        List<ServiceUnit> serviceUnits = serviceUnitRepository.orderByName();
        long orderingId = 1;

        for (ServiceUnit su : serviceUnits) {
            su.setOrderingId(orderingId);
            orderingId++;
        }

        serviceUnitRepository.saveAll(serviceUnits);
    }

    /**
     * Deletes {@link ServiceUnit} if the validations are passed.
     *
     * @param id ID of the {@link ServiceUnit}
     * @throws DomainEntityNotFoundException if {@link ServiceUnit} is not found.
     * @throws OperationNotAllowedException if the {@link ServiceUnit} is already deleted.
     * @throws OperationNotAllowedException if the {@link ServiceUnit} is connected to active object.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_UNITS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Transactional
    @Override
    public void delete(Long id) {
        log.debug("Removing service unit with ID: {}", id);

        ServiceUnit serviceUnit = serviceUnitRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service unit not found, ID: " + id));

        if (serviceUnit.getStatus().equals(DELETED)) {
            log.error("Service unit {} is already deleted", id);
            throw new OperationNotAllowedException("id-Service unit " + id + " is already deleted");
        }

        Long activeConnections = serviceUnitRepository.activeConnectionCount(
                id,
                List.of(ServiceDetailStatus.ACTIVE,ServiceDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        // TODO: 23.02.23 uncomment and implement later
//        if (serviceUnitRepository.getActiveConnectionsCount(id) > 0) {
//            log.error("Service unit {} is connected to active object, cannot be deleted", id);
//            throw new OperationNotAllowedException("id-Service unit " + id + " is connected to active object, cannot be deleted");
//        }

        serviceUnit.setStatus(DELETED);
        serviceUnitRepository.save(serviceUnit);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return serviceUnitRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return serviceUnitRepository.findByIdIn(ids);
    }

    /**
     * Filters {@link ServiceUnit} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceUnit#name}.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ServiceUnitResponse> Page&lt;ServiceUnitResponse&gt;} containing detailed information
     */
    public Page<ServiceUnitResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering service units list with request: {}", request);
        Page<ServiceUnit> serviceUnits = serviceUnitRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
        return serviceUnits.map(serviceUnitMapper::responseFromEntity);
    }

    /**
     * Adds {@link ServiceUnit} at the end with the highest ordering ID.
     * If the request asks to save {@link ServiceUnit} as a default and a default {@link ServiceUnit} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ServiceUnitRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link ServiceUnitResponse}
     */
    @Transactional
    public ServiceUnitResponse add(ServiceUnitRequest request) {
        log.debug("Adding service unit: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (serviceUnitRepository.countServiceUnitsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Service unit with such name already exists");
            throw new ClientException("name-Service unit with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = serviceUnitRepository.findLastOrderingId();
        ServiceUnit serviceUnit = serviceUnitMapper.entityFromRequest(request);
        serviceUnit.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, serviceUnit);

        serviceUnitRepository.save(serviceUnit);
        return serviceUnitMapper.responseFromEntity(serviceUnit);
    }

    /**
     * Sets the default selection flag for the given {@link ServiceUnit} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link ServiceUnit} is set to false,
     * and the given {@link ServiceUnit} is set as the new default selection.
     *
     * @param request the {@link ServiceUnitRequest} containing the status and default selection flag to use when setting the default selection
     * @param serviceUnit the {@link ServiceUnit} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(ServiceUnitRequest request, ServiceUnit serviceUnit) {
        if (request.getStatus().equals(INACTIVE)) {
            serviceUnit.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<ServiceUnit> currentDefaultServiceUnitOptional = serviceUnitRepository.findByDefaultSelectionTrue();
                if (currentDefaultServiceUnitOptional.isPresent()) {
                    ServiceUnit currentDefaultServiceUnit = currentDefaultServiceUnitOptional.get();
                    currentDefaultServiceUnit.setDefaultSelection(false);
                    serviceUnitRepository.save(currentDefaultServiceUnit);
                }
            }
            serviceUnit.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Retrieves detailed information about {@link ServiceUnit} by ID
     *
     * @param id ID of {@link ServiceUnit}
     * @return {@link ServiceUnitResponse}
     * @throws DomainEntityNotFoundException if no {@link ServiceUnit} was found with the provided ID.
     */
    public ServiceUnitResponse view(Long id) {
        log.debug("Fetching service units with ID: {}", id);
        ServiceUnit serviceUnit = serviceUnitRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service unit not found, ID: " + id));
        return serviceUnitMapper.responseFromEntity(serviceUnit);
    }

    /**
     * Edit the requested {@link ServiceUnit}.
     * If the request asks to save {@link ServiceUnit} as a default and a default {@link ServiceUnit} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link ServiceUnit}
     * @param request {@link ServiceUnitRequest}
     * @throws DomainEntityNotFoundException if {@link ServiceUnit} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link ServiceUnit} is deleted.
     * @return {@link ServiceUnitResponse}
     */
    @Transactional
    public ServiceUnitResponse edit(Long id, ServiceUnitRequest request) {
        log.debug("Editing service unit: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ServiceUnit serviceUnit = serviceUnitRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service unit not found, ID: " + id));

        if (serviceUnit.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (serviceUnitRepository.countServiceUnitsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !serviceUnit.getName().equals(request.getName().trim())) {
            log.error("Service unit with such name already exists");
            throw new ClientException("name-Service unit with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, serviceUnit);

        serviceUnit.setName(request.getName().trim());
        serviceUnit.setStatus(request.getStatus());

        return serviceUnitMapper.responseFromEntity(serviceUnit);
    }

    /**
     * Sets the default selection flag for the given {@link ServiceUnit} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link ServiceUnit} is set to false,
     * and the given {@link ServiceUnit} is set as the new default selection.
     *
     * @param request the {@link ServiceUnitRequest} containing the status and default selection flag to use when setting the default selection
     * @param serviceUnit the {@link ServiceUnit} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(ServiceUnitRequest request, ServiceUnit serviceUnit) {
        if (request.getStatus().equals(INACTIVE)) {
            serviceUnit.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!serviceUnit.isDefaultSelection()) {
                    Optional<ServiceUnit> currentDefaultServiceUnitOptional = serviceUnitRepository.findByDefaultSelectionTrue();
                    if (currentDefaultServiceUnitOptional.isPresent()) {
                        ServiceUnit currentDefaultServiceUnit = currentDefaultServiceUnitOptional.get();
                        currentDefaultServiceUnit.setDefaultSelection(false);
                        serviceUnitRepository.save(currentDefaultServiceUnit);
                    }
                }
            }
            serviceUnit.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
