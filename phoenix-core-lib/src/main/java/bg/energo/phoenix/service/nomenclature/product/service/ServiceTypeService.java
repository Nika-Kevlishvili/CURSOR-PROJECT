package bg.energo.phoenix.service.nomenclature.product.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.service.ServiceTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ServiceTypeResponse;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICE_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTypeService implements NomenclatureBaseService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SERVICE_TYPE;
    }


    /**
     * Filters {@link ServiceType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_TYPES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Service type list with statuses: {}", request);
        return serviceTypeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "orderingId"))
                );
    }


    /**
     * Changes the ordering of a {@link ServiceType} item in the Service type list to a specified position.
     * The method retrieves the {@link ServiceType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ServiceType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ServiceType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        ServiceType serviceType = serviceTypeRepository
                .findByIdAndStatuses(request.getId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service type not found"));

        Long start;
        Long end;
        List<ServiceType> serviceTypes;

        if (serviceType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = serviceType.getOrderingId();
            serviceTypes = serviceTypeRepository.findInOrderingIdRange(start, end, serviceType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (ServiceType b : serviceTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = serviceType.getOrderingId();
            end = request.getOrderingId();
            serviceTypes = serviceTypeRepository.findInOrderingIdRange(start, end, serviceType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ServiceType b : serviceTypes) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        serviceType.setOrderingId(request.getOrderingId());
        serviceTypes.add(serviceType);
        serviceTypeRepository.saveAll(serviceTypes);
    }


    /**
     * Sorts all {@link ServiceType} alphabetically.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Service types alphabetically");
        List<ServiceType> serviceTypes = serviceTypeRepository.orderByName();
        long orderingId = 1;

        for (ServiceType b : serviceTypes) {
            b.setOrderingId(orderingId);
            orderingId++;
        }
        serviceTypeRepository.saveAll(serviceTypes);
    }


    /**
     * Deletes {@link ServiceType}
     *
     * @param id ID of the {@link ServiceType}
     * @throws DomainEntityNotFoundException if {@link ServiceType} is not found.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SERVICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Service type  with ID: {}", id);
        ServiceType elType = serviceTypeRepository
                .findByIdAndStatuses(id, List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service type not found"));

        Long activeConnections = serviceTypeRepository.activeConnectionCount(
                id,
                List.of(ServiceDetailStatus.ACTIVE,ServiceDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        elType.setStatus(DELETED);
        serviceTypeRepository.save(elType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return serviceTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return serviceTypeRepository.findByIdIn(ids);
    }


    /**
     * Adds {@link ServiceType} at the end with the highest ordering ID.
     * {@link ServiceType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param request {@link ServiceTypeRequest}
     * @return {@link ServiceTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ServiceTypeResponse add(ServiceTypeRequest request) {
        log.debug("Adding Service type: {}", request.toString());

        validateRequest(request);
        serviceTypeRepository.findByNameAndStatuses(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                .ifPresent(elWIthName -> {
                    String message = String.format("name-Service type with the name %s already exists", elWIthName.getName());
                    log.error(message);
                    throw new ClientException(message, ErrorCode.CONFLICT);
                });
        Long lastSortOrder = serviceTypeRepository.findLastOrderingId();
        ServiceType serviceType = new ServiceType(request);
        serviceType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (serviceType.isDefaultSelection()) {
            Optional<ServiceType> currentDefaultOptional = serviceTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                ServiceType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                serviceTypeRepository.save(currentDefault);
            }
        }
        ServiceType serviceTypeEntity = serviceTypeRepository.save(serviceType);
        return new ServiceTypeResponse(serviceTypeEntity);
    }


    /**
     * Edit the requested {@link ServiceType}.
     * {@link ServiceType} with already existing name and status Active or Inactive can not be saved.
     *
     * @param id      ID of {@link ServiceType}
     * @param request {@link ServiceTypeRequest}
     * @return {@link ServiceTypeResponse}
     * @throws DomainEntityNotFoundException if {@link ServiceType} is not found with Active or Inactive status.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request has status DELETED.
     */
    @Transactional
    public ServiceTypeResponse edit(Long id, ServiceTypeRequest request) {
        log.debug("Adding Service type: {}", request.toString());

        validateRequest(request);


        ServiceType elType = serviceTypeRepository
                .findByIdAndStatuses(id,List.of(ACTIVE,INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service not found"));

        serviceTypeRepository.findByNameAndStatuses(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                .ifPresent(elWIthName -> {
                    if (!elWIthName.getId().equals(elType.getId())){
                        String message = String.format("name-Service type with the name %s already exists", elWIthName.getName());
                        log.error(message);
                        throw new ClientException(message, ErrorCode.CONFLICT);
                    }
                });
        if (request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection()&& !elType.isDefaultSelection()) {

            Optional<ServiceType> currentDefaultOptional = serviceTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultOptional.isPresent()) {
                ServiceType currentDefault = currentDefaultOptional.get();
                currentDefault.setDefaultSelection(false);
                serviceTypeRepository.save(currentDefault);
            }
            elType.setDefaultSelection(true);
        }
        elType.setName(request.getName().trim());
        elType.setDefaultSelection(request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection());
        elType.setStatus(request.getStatus());
        return new ServiceTypeResponse(serviceTypeRepository.save(elType));
    }


    /**
     * Filters {@link ServiceType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ServiceTypeResponse> Page&lt;ServiceTypeResponse&gt;} containing detailed information
     */
    public Page<ServiceTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Service types list with : {}", request.toString());
        Page<ServiceType> page = serviceTypeRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize(),Sort.by(Sort.Direction.ASC,"orderingId"))
        );
        return page.map(ServiceTypeResponse::new);
    }


    /**
     * Retrieves detailed information about {@link ServiceType} by ID
     *
     * @param id ID of {@link ServiceType}
     * @return {@link ServiceTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link ServiceType} was found with the provided ID.
     */
    public ServiceTypeResponse view(Long id) {
        log.debug("Fetching Service type with ID: {}", id);
        ServiceType elType = serviceTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service type not found"));
        return new ServiceTypeResponse(elType);
    }


    /**
     * Validates request checks if:
     * 1. request status is not deleted
     * 2. Service type with name does not exist
     *
     * @param request to validate {@link ServiceTypeRequest}
     * @throws ClientException with ErrorCode ILLEGAL_ARGUMENTS_PROVIDED when status in request is Deleted
     * @throws ClientException with ErrorCode CONFLICT when another {@link ServiceType} exists with same name
     */
    private void validateRequest(ServiceTypeRequest request) {
        if (request.getStatus().equals(DELETED)) {
            String msg = "status-Cannot add item with status DELETED";
            log.error(msg);
            throw new ClientException(msg,ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

    }
}
