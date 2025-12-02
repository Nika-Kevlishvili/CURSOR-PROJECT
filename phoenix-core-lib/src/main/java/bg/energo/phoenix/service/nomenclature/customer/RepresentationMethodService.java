package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.RepresentationMethod;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.RepresentationMethodRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.RepresentationMethodResponse;
import bg.energo.phoenix.repository.nomenclature.customer.RepresentationMethodRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.REPRESENTATION_METHODS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepresentationMethodService implements NomenclatureBaseService {

    private final RepresentationMethodRepository representationMethodRepository;

    /**
     * Filters {@link RepresentationMethod} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link RepresentationMethod}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<RepresentationMethodResponse> Page&lt;RepresentationMethodResponse&gt;} containing detailed information
     */
    public Page<RepresentationMethodResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering REPRESENTATION_METHODS list with request: {}", request.toString());
        Page<RepresentationMethod> page = representationMethodRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(RepresentationMethodResponse::new);
    }

    /**
     * Filters {@link RepresentationMethod} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link RepresentationMethod}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REPRESENTATION_METHODS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering REPRESENTATION_METHODS list with statuses: {}", request);
        return representationMethodRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    /**
     * Adds {@link RepresentationMethod} at the end with the highest ordering ID.
     * If the request asks to save {@link RepresentationMethod} as a default and a default {@link RepresentationMethod} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link RepresentationMethod}
     * @return {@link RepresentationMethodResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public RepresentationMethodResponse add(RepresentationMethodRequest request) {
        log.debug("Adding RepresentationMethod: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (representationMethodRepository.countRepresentationMethodByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-RepresentationMethod with the same name already exists;");
            throw new OperationNotAllowedException("name-RepresentationMethod with the same name already exists;");
        }

        Long lastSortOrder = representationMethodRepository.findLastOrderingId();
        RepresentationMethod representationMethod = new RepresentationMethod(request);
        representationMethod.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<RepresentationMethod> currentDefaultRepresentationMethodOptional =
                    representationMethodRepository.findByDefaultSelectionTrue();
            if (currentDefaultRepresentationMethodOptional.isPresent()) {
                RepresentationMethod currentDefaultRepresentationMethod =
                        currentDefaultRepresentationMethodOptional.get();
                currentDefaultRepresentationMethod.setDefaultSelection(false);
                representationMethodRepository.save(currentDefaultRepresentationMethod);
            }
        }
        RepresentationMethod representationMethodEntity = representationMethodRepository.save(representationMethod);
        return new RepresentationMethodResponse(representationMethodEntity);
    }

    /**
     * Retrieves detailed information about {@link RepresentationMethod} by ID
     *
     * @param id ID of {@link RepresentationMethod}
     * @return {@link RepresentationMethodResponse}
     * @throws DomainEntityNotFoundException if no {@link RepresentationMethod} was found with the provided ID.
     */
    public RepresentationMethodResponse view(Long id) {
        log.debug("Getting manage method with id : {}", id);
        RepresentationMethod representationMethod = representationMethodRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new RepresentationMethodResponse(representationMethod);
    }

    /**
     * Edit the requested {@link RepresentationMethod}.
     * If the request asks to save {@link RepresentationMethod} as a default and a default {@link RepresentationMethod} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link RepresentationMethod}
     * @param request {@link RepresentationMethodRequest}
     * @return {@link RepresentationMethodResponse}
     * @throws DomainEntityNotFoundException if {@link RepresentationMethod} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link RepresentationMethod} is deleted.
     */
    @Transactional
    public RepresentationMethodResponse edit(Long id, RepresentationMethodRequest request) {
        log.debug("Editing RepresentationMethod: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        RepresentationMethod representationMethod = representationMethodRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (representationMethodRepository.countRepresentationMethodByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !representationMethod.getName().equals(request.getName().trim())) {
            log.error("name-RepresentationMethod with the same name already exists;");
            throw new OperationNotAllowedException("name-RepresentationMethod with the same name already exists;");
        }

        if (representationMethod.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !representationMethod.isDefaultSelection()) {
            Optional<RepresentationMethod> currentDefaultrepresentationMethodOptional =
                    representationMethodRepository.findByDefaultSelectionTrue();
            if (currentDefaultrepresentationMethodOptional.isPresent()) {
                RepresentationMethod currentDefaultRepMethod = currentDefaultrepresentationMethodOptional.get();
                currentDefaultRepMethod.setDefaultSelection(false);
                representationMethodRepository.save(currentDefaultRepMethod);
            }
        }
        representationMethod.setDefaultSelection(request.getDefaultSelection());

        representationMethod.setName(request.getName().trim());
        representationMethod.setStatus(request.getStatus());
        return new RepresentationMethodResponse(representationMethodRepository.save(representationMethod));
    }

    /**
     * Deletes {@link RepresentationMethod} if the validations are passed.
     *
     * @param id ID of the {@link RepresentationMethod}
     * @throws DomainEntityNotFoundException if {@link RepresentationMethod} is not found.
     * @throws OperationNotAllowedException  if the {@link RepresentationMethod} is already deleted.
     * @throws OperationNotAllowedException  if the {@link RepresentationMethod} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REPRESENTATION_METHODS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        // TODO: Check if there is no connected object to this nomenclature item in system
        log.debug("Removing Manage Method with ID: {}", id);
        RepresentationMethod representationMethod = representationMethodRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (representationMethod.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (representationMethodRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        representationMethod.setStatus(DELETED);
        representationMethodRepository.save(representationMethod);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return representationMethodRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return representationMethodRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link RepresentationMethod} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REPRESENTATION_METHODS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the representation methods alphabetically");
        List<RepresentationMethod> representationMethods = representationMethodRepository.orderByName();
        long orderingId = 1;

        for (RepresentationMethod c : representationMethods) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        representationMethodRepository.saveAll(representationMethods);
    }

    /**
     * Changes the ordering of a {@link RepresentationMethod} item in the RepresentationMethod list to a specified position.
     * The method retrieves the {@link RepresentationMethod} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link RepresentationMethod} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link RepresentationMethod} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = REPRESENTATION_METHODS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in representation method to top", request.getId());

        RepresentationMethod representationMethod = representationMethodRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<RepresentationMethod> representationMethodList;

        if (representationMethod.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = representationMethod.getOrderingId();
            representationMethodList = representationMethodRepository.findInOrderingIdRange(
                    start,
                    end,
                    representationMethod.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (RepresentationMethod rm : representationMethodList) {
                rm.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = representationMethod.getOrderingId();
            end = request.getOrderingId();
            representationMethodList = representationMethodRepository.findInOrderingIdRange(
                    start,
                    end,
                    representationMethod.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (RepresentationMethod rm : representationMethodList) {
                rm.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        representationMethod.setOrderingId(request.getOrderingId());
        representationMethodRepository.save(representationMethod);
        representationMethodRepository.saveAll(representationMethodList);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.REPRESENTATION_METHODS;
    }
}
