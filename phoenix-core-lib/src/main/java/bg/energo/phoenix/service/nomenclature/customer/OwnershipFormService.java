package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.OwnershipForm;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.OwnershipFormRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.OwnershipFormResponse;
import bg.energo.phoenix.repository.nomenclature.customer.OwnershipFormRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipFormService implements NomenclatureBaseService {
    private final OwnershipFormRepository ownershipFormRepository;

    /**
     * Filters {@link OwnershipForm} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link OwnershipForm}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<OwnershipFormResponse> Page&lt;OwnershipFormResponse&gt;} containing detailed information
     */
    public Page<OwnershipFormResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering OWNERSHIP_FORM list with request: {}", request.toString());
        Page<OwnershipForm> page = ownershipFormRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(OwnershipFormResponse::new);
    }

    /**
     * Filters {@link OwnershipForm} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link OwnershipForm}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = OWNERSHIP_FORM, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ownership form list with statuses: {}", request);
        return ownershipFormRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), request.getStatuses(), PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Adds {@link OwnershipForm} at the end with the highest ordering ID.
     * If the request asks to save {@link OwnershipForm} as a default and a default {@link OwnershipForm} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link OwnershipForm}
     * @return {@link OwnershipFormResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public OwnershipFormResponse add(OwnershipFormRequest request) {
        log.debug("Adding Ownership form: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (ownershipFormRepository.countOwnershipFormByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-OwnershipForm with the same name already exists;");
            throw new OperationNotAllowedException("name-OwnershipForm with the same name already exists;");
        }

        Long lastSortOrder = ownershipFormRepository.findLastOrderingId();
        OwnershipForm ownershipForm = new OwnershipForm(request);
        ownershipForm.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<OwnershipForm> currentDefaultOwnershipFormOptional = ownershipFormRepository.findByDefaultSelectionTrue();
            if (currentDefaultOwnershipFormOptional.isPresent()) {
                OwnershipForm currentDefaultOwnershipForm = currentDefaultOwnershipFormOptional.get();
                currentDefaultOwnershipForm.setDefaultSelection(false);
                ownershipFormRepository.save(currentDefaultOwnershipForm);
            }
        }
        OwnershipForm OwnershipFormEntity = ownershipFormRepository.save(ownershipForm);
        return new OwnershipFormResponse(OwnershipFormEntity);
    }

    /**
     * Retrieves detailed information about {@link OwnershipForm} by ID
     *
     * @param id ID of {@link OwnershipForm}
     * @return {@link OwnershipFormResponse}
     * @throws DomainEntityNotFoundException if no {@link OwnershipForm} was found with the provided ID.
     */
    public OwnershipFormResponse view(Long id) {
        log.debug("Fetching ownership form with ID: {}", id);
        OwnershipForm ownershipForm = ownershipFormRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new OwnershipFormResponse(ownershipForm);
    }

    /**
     * Edit the requested {@link OwnershipForm}.
     * If the request asks to save {@link OwnershipForm} as a default and a default {@link OwnershipForm} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link OwnershipForm}
     * @param request {@link OwnershipFormRequest}
     * @return {@link OwnershipFormResponse}
     * @throws DomainEntityNotFoundException if {@link OwnershipForm} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link OwnershipForm} is deleted.
     */
    @Transactional
    public OwnershipFormResponse edit(Long id, OwnershipFormRequest request) {
        log.debug("Editing ownership form: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        OwnershipForm ownershipForm = ownershipFormRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (ownershipFormRepository.countOwnershipFormByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !ownershipForm.getName().equals(request.getName().trim())) {
            log.error("name-OwnershipForm with the same name already exists;");
            throw new OperationNotAllowedException("name-OwnershipForm with the same name already exists;");
        }


        if (ownershipForm.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !ownershipForm.isDefaultSelection()) {
            Optional<OwnershipForm> currentDefaultOwnershipFormOptional = ownershipFormRepository.findByDefaultSelectionTrue();
            if (currentDefaultOwnershipFormOptional.isPresent()) {
                OwnershipForm currentDefaultOwnershipForm = currentDefaultOwnershipFormOptional.get();
                currentDefaultOwnershipForm.setDefaultSelection(false);
                ownershipFormRepository.save(currentDefaultOwnershipForm);
            }
        }
        ownershipForm.setDefaultSelection(request.getDefaultSelection());

        ownershipForm.setName(request.getName().trim());
        ownershipForm.setStatus(request.getStatus());
        return new OwnershipFormResponse(ownershipFormRepository.save(ownershipForm));
    }

    /**
     * Deletes {@link OwnershipForm} if the validations are passed.
     *
     * @param id ID of the {@link OwnershipForm}
     * @throws DomainEntityNotFoundException if {@link OwnershipForm} is not found.
     * @throws OperationNotAllowedException  if the {@link OwnershipForm} is already deleted.
     * @throws OperationNotAllowedException  if the {@link OwnershipForm} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = OWNERSHIP_FORM, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing ownership form with ID: {}", id);
        OwnershipForm ownershipForm = ownershipFormRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (ownershipForm.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (ownershipFormRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        ownershipForm.setStatus(DELETED);
        ownershipFormRepository.save(ownershipForm);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return ownershipFormRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return ownershipFormRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link OwnershipForm} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = OWNERSHIP_FORM, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        // TODO: 13.12.22 add system user id

        log.debug("Sorting the ownership form alphabetically");
        List<OwnershipForm> ownershipForms = ownershipFormRepository.orderByName();
        long orderingId = 1;

        for (OwnershipForm c : ownershipForms) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        ownershipFormRepository.saveAll(ownershipForms);
    }

    /**
     * Changes the ordering of a {@link OwnershipForm} item in the OwnershipForm list to a specified position.
     * The method retrieves the {@link OwnershipForm} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link OwnershipForm} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link OwnershipForm} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = OWNERSHIP_FORM, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        // TODO: 13.12.22 add system user id

        log.debug("Moving item with ID: {} in Ownership forms to top", request.getId());

        OwnershipForm ownershipForm = ownershipFormRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<OwnershipForm> ownershipForms;

        if (ownershipForm.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = ownershipForm.getOrderingId();
            ownershipForms = ownershipFormRepository.findInOrderingIdRange(
                    start,
                    end,
                    ownershipForm.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (OwnershipForm c : ownershipForms) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = ownershipForm.getOrderingId();
            end = request.getOrderingId();
            ownershipForms = ownershipFormRepository.findInOrderingIdRange(
                    start,
                    end,
                    ownershipForm.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (OwnershipForm c : ownershipForms) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        ownershipForm.setOrderingId(request.getOrderingId());
        ownershipFormRepository.save(ownershipForm);
        ownershipFormRepository.saveAll(ownershipForms);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.OWNERSHIP_FORM;
    }
}
