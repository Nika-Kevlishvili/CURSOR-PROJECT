package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.ContactPurposeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.ContactPurposeResponse;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CONTACT_PURPOSE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactPurposeService implements NomenclatureBaseService {
    private final ContactPurposeRepository contactPurposeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CONTACT_PURPOSE;
    }

    /**
     * Filters {@link ContactPurpose} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ContactPurpose}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ContactPurposeResponse> Page&lt;ContactPurposeResponse&gt;} containing detailed information
     */
    public Page<ContactPurposeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Contract purpose list with request: {}", request.toString());
        Page<ContactPurpose> page = contactPurposeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ContactPurposeResponse::new);
    }

    /**
     * Filters {@link ContactPurpose} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ContactPurpose}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTACT_PURPOSE, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering contact purpose nomenclature with request: {}", request.toString());
        return contactPurposeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link ContactPurpose} at the end with the highest ordering ID.
     * If the request asks to save {@link ContactPurpose} as a default and a default {@link ContactPurpose} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ContactPurpose}
     * @return {@link ContactPurposeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ContactPurposeResponse add(ContactPurposeRequest request) {
        log.debug("Adding ContactPurpose: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot set DELETED status",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (contactPurposeRepository.countContactPurposeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-ContactPurpose with the same name already exists;");
            throw new OperationNotAllowedException("name-ContactPurpose with the same name already exists;");
        }

        Long lastSortOrder = contactPurposeRepository.findLastOrderingId();
        ContactPurpose contactPurpose = new ContactPurpose(request);
        contactPurpose.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<ContactPurpose> currentDefaultContactPurposeOptional =
                    contactPurposeRepository.findByDefaultSelectionTrue();
            if (currentDefaultContactPurposeOptional.isPresent()) {
                ContactPurpose currentDefaultContactPurpose =
                        currentDefaultContactPurposeOptional.get();
                currentDefaultContactPurpose.setDefaultSelection(false);
                contactPurposeRepository.save(currentDefaultContactPurpose);
            }
        }
        contactPurpose.setIsHardCoded(false);
        ContactPurpose contactPurposeEntity = contactPurposeRepository.save(contactPurpose);
        return new ContactPurposeResponse(contactPurposeEntity);
    }

    /**
     * Retrieves detailed information about {@link ContactPurpose} by ID
     *
     * @param id ID of {@link ContactPurpose}
     * @return {@link ContactPurposeResponse}
     * @throws DomainEntityNotFoundException if no {@link ContactPurpose} was found with the provided ID.
     */
    public ContactPurposeResponse view(Long id) {
        log.debug("Fetching ContactPurpose with ID: {}", id);
        ContactPurpose contactPurpose = contactPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new ContactPurposeResponse(contactPurpose);
    }

    /**
     * Edit the requested {@link ContactPurpose}.
     * If the request asks to save {@link ContactPurpose} as a default and a default {@link ContactPurpose} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link ContactPurpose}
     * @param request {@link ContactPurposeRequest}
     * @return {@link ContactPurposeResponse}
     * @throws DomainEntityNotFoundException if {@link ContactPurpose} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link ContactPurpose} is deleted.
     */
    @Transactional
    public ContactPurposeResponse edit(Long id, ContactPurposeRequest request) {
        log.debug("Editing ContactPurpose: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ContactPurpose contactPurpose = contactPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (contactPurposeRepository.countContactPurposeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !contactPurpose.getName().equals(request.getName().trim())) {
            log.error("name-ContactPurpose with the same name already exists;");
            throw new OperationNotAllowedException("name-ContactPurpose with the same name already exists;");
        }


        if (contactPurpose.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.;");
        }
        if(contactPurpose.getIsHardCoded()){
            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be changed.;");
        }

        if (request.getDefaultSelection() && !contactPurpose.isDefaultSelection()) {
            Optional<ContactPurpose> currentDefaultContactPurposeOptional =
                    contactPurposeRepository.findByDefaultSelectionTrue();
            if (currentDefaultContactPurposeOptional.isPresent()) {
                ContactPurpose currentDefaultContactPurpose =
                        currentDefaultContactPurposeOptional.get();
                currentDefaultContactPurpose.setDefaultSelection(false);
                contactPurposeRepository.save(currentDefaultContactPurpose);
            }
        }
        contactPurpose.setDefaultSelection(request.getDefaultSelection());

        contactPurpose.setName(request.getName().trim());
        contactPurpose.setStatus(request.getStatus());
        return new ContactPurposeResponse(contactPurposeRepository.save(contactPurpose));
    }

    /**
     * Deletes {@link ContactPurpose} if the validations are passed.
     *
     * @param id ID of the {@link ContactPurpose}
     * @throws DomainEntityNotFoundException if {@link ContactPurpose} is not found.
     * @throws OperationNotAllowedException  if the {@link ContactPurpose} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ContactPurpose} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTACT_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        // TODO: 26.11.22 Check if there is no connected object to this nomenclature item in system
        log.debug("Removing contactPurpose with ID: {}", id);
        ContactPurpose contactPurpose = contactPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if(contactPurpose.getIsHardCoded()){
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }
        if (contactPurpose.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (contactPurposeRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        contactPurpose.setStatus(DELETED);
        contactPurposeRepository.save(contactPurpose);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return contactPurposeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return contactPurposeRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link ContactPurpose} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTACT_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the contactPurpose alphabetically");
        List<ContactPurpose> contactPurposes = contactPurposeRepository.orderByName();
        long orderingId = 1;

        for (ContactPurpose c : contactPurposes) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        contactPurposeRepository.saveAll(contactPurposes);
    }

    /**
     * Changes the ordering of a {@link ContactPurpose} item in the ContactPurpose list to a specified position.
     * The method retrieves the {@link ContactPurpose} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link ContactPurpose} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ContactPurpose} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTACT_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in contactPurpose to place: {}", request.getId(), request.getOrderingId());

        ContactPurpose contactPurpose = contactPurposeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ContactPurpose> contactPurposeList;

        if (contactPurpose.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = contactPurpose.getOrderingId();

            contactPurposeList = contactPurposeRepository.findInOrderingIdRange(
                    start,
                    end,
                    contactPurpose.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ContactPurpose c : contactPurposeList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = contactPurpose.getOrderingId();
            end = request.getOrderingId();

            contactPurposeList = contactPurposeRepository.findInOrderingIdRange(
                    start,
                    end,
                    contactPurpose.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ContactPurpose c : contactPurposeList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        contactPurpose.setOrderingId(request.getOrderingId());
        contactPurposeList.add(contactPurpose);
        contactPurposeRepository.saveAll(contactPurposeList);
    }
}
