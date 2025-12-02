package bg.energo.phoenix.service.nomenclature.product.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.service.ServiceGroupsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ServiceGroupsResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceGroupsRepository;
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
public class ServiceGroupsService implements NomenclatureBaseService {
    private final ServiceGroupsRepository serviceGroupsRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SERVICE_GROUPS;
    }

    /**
     * Adds {@link ServiceGroups} at the end with the highest ordering ID.
     * If the request asks to save {@link ServiceGroups} as a default and a default {@link ServiceGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ServiceGroupsRequest}
     * @return {@link ServiceGroupsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ServiceGroupsResponse add(ServiceGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());
        log.debug("Adding Service Groups: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<ServiceGroups> serviceGroupsWithName = serviceGroupsRepository.findByNameAndStatus(request.getName(), List.of(ACTIVE, INACTIVE));
        if (serviceGroupsWithName.size() > 0) {
            log.error("Service Group with presented name already exists");
            throw new ClientException("name-Service Group with presented name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = serviceGroupsRepository.findLastOrderingId();
        ServiceGroups serviceGroups = new ServiceGroups(request);
        serviceGroups.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, serviceGroups);
        ServiceGroups savedGroup = serviceGroupsRepository.save(serviceGroups);
        return new ServiceGroupsResponse(savedGroup);
    }

    /**
     * Edit the requested {@link ServiceGroups}.
     * If the request asks to save {@link ServiceGroups} as a default and a default {@link ServiceGroups} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link ServiceGroups}
     * @param request {@link ServiceGroupsRequest}
     * @return {@link ServiceGroupsResponse}
     * @throws DomainEntityNotFoundException if {@link ServiceGroups} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link ServiceGroups} is deleted.
     */
    @Transactional
    public ServiceGroupsResponse edit(Long id, ServiceGroupsRequest request) {
        request.setName(request.getName().trim());
        request.setNameTransliterated(request.getNameTransliterated().trim());
        log.debug("Editing Service Group: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ServiceGroups serviceGroups = serviceGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Service Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        if (!serviceGroups.getName().equalsIgnoreCase(request.getName())) {
            List<ServiceGroups> serviceGroupsOptional = serviceGroupsRepository.findByNameAndStatus(request.getName(), List.of(ACTIVE, INACTIVE));
            if (serviceGroupsOptional.size() > 0) {
                log.error("Service Group with presented name already exists");
                throw new ClientException("name-Service Group with presented name already exists, cannot edit requested service group", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (serviceGroups.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, serviceGroups);

        serviceGroups.setName(request.getName());
        serviceGroups.setNameTransliterated(request.getNameTransliterated());
        serviceGroups.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            serviceGroups.setDefaultSelection(false);
        }
        return new ServiceGroupsResponse(serviceGroupsRepository.save(serviceGroups));
    }

    private void checkCurrentDefaultSelection(ServiceGroupsRequest request, ServiceGroups serviceGroups) {
        if (request.getStatus().equals(INACTIVE)) {
            serviceGroups.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<ServiceGroups> currentDefaultServiceGroupsOptional = serviceGroupsRepository.findByDefaultSelectionTrue();
                if (currentDefaultServiceGroupsOptional.isPresent()) {
                    ServiceGroups defaultServiceGroups = currentDefaultServiceGroupsOptional.get();
                    defaultServiceGroups.setDefaultSelection(false);
                    serviceGroupsRepository.save(defaultServiceGroups);
                }
            }
            serviceGroups.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Deletes {@link ServiceGroups} if the validations are passed.
     *
     * @param id ID of the {@link ServiceGroups}
     * @throws DomainEntityNotFoundException if {@link ServiceGroups} is not found.
     * @throws OperationNotAllowedException  if the {@link ServiceGroups} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ServiceGroups} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.SERVICE_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Service Group with ID: {}", id);
        ServiceGroups serviceGroups = serviceGroupsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service Group not found"));

        if (serviceGroups.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        Long activeConnections = serviceGroupsRepository.activeConnectionCount(
                id,
                List.of(ServiceDetailStatus.ACTIVE,ServiceDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        serviceGroups.setDefaultSelection(false);
        serviceGroups.setStatus(DELETED);
        serviceGroupsRepository.save(serviceGroups);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return serviceGroupsRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return serviceGroupsRepository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link ServiceGroupsResponse} by ID
     *
     * @param id ID of {@link ServiceGroups}
     * @return {@link ServiceGroupsResponse}
     * @throws DomainEntityNotFoundException if no {@link ServiceGroups} was found with the provided ID.
     */
    public ServiceGroupsResponse view(Long id) {
        log.debug("Fetching Service Group with ID: {}", id);
        ServiceGroups serviceGroups = serviceGroupsRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Service Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        return new ServiceGroupsResponse(serviceGroups);
    }

    /**
     * Filters {@link ServiceGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ServiceGroupsResponse> Page&lt;ServiceGroupsResponse&gt;} containing detailed information
     */
    public Page<ServiceGroupsResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Service Groups list with request: {}", request.toString());
        Page<ServiceGroups> page = serviceGroupsRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ServiceGroupsResponse::new);
    }

    /**
     * Filters {@link ServiceGroups} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ServiceGroups}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.SERVICE_GROUPS, permissions = {PermissionEnum.NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Service Groups list with statuses: {}", request);
        return serviceGroupsRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link ServiceGroups} item in the ServiceGroups list to a specified position.
     * The method retrieves the {@link ServiceGroups} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ServiceGroups} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ServiceGroups} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.SERVICE_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in Service Groups to top", request.getId());

        ServiceGroups serviceGroup = serviceGroupsRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-Service Group with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ServiceGroups> serviceGroupsList;

        if (serviceGroup.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = serviceGroup.getOrderingId();
            serviceGroupsList = serviceGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    serviceGroup.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ServiceGroups sg : serviceGroupsList) {
                sg.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = serviceGroup.getOrderingId();
            end = request.getOrderingId();
            serviceGroupsList = serviceGroupsRepository.findInOrderingIdRange(
                    start,
                    end,
                    serviceGroup.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ServiceGroups sg : serviceGroupsList) {
                sg.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        serviceGroup.setOrderingId(request.getOrderingId());
        serviceGroupsRepository.save(serviceGroup);
        serviceGroupsRepository.saveAll(serviceGroupsList);
    }

    /**
     * Sorts all {@link ServiceGroups} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.SERVICE_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Service Groups alphabetically");
        List<ServiceGroups> serviceGroups = serviceGroupsRepository.orderByName();
        long orderingId = 1;

        for (ServiceGroups groups : serviceGroups) {
            groups.setOrderingId(orderingId);
            orderingId++;
        }

        serviceGroupsRepository.saveAll(serviceGroups);
    }
}
