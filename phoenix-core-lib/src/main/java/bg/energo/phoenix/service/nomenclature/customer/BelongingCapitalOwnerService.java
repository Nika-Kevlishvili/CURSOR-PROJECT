package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.BelongingCapitalOwner;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.BelongingCapitalOwnerRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BelongingCapitalOwnerResponse;
import bg.energo.phoenix.repository.nomenclature.customer.BelongingCapitalOwnerRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.BELONGING_CAPITAL_OWNERS;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BelongingCapitalOwnerService implements NomenclatureBaseService {
    private final BelongingCapitalOwnerRepository belongingCapitalOwnerRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.BELONGING_CAPITAL_OWNERS;
    }

    /**
     * Filters {@link BelongingCapitalOwner} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link BelongingCapitalOwner}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BELONGING_CAPITAL_OWNERS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Belonging Capital Owners list with statuses: {}", request);
        return belongingCapitalOwnerRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link BelongingCapitalOwner} item in the BelongingCapitalOwner list to a specified position.
     * The method retrieves the {@link BelongingCapitalOwner} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link BelongingCapitalOwner} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link BelongingCapitalOwner} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BELONGING_CAPITAL_OWNERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in belonging capital owner to top", request.getId());

        BelongingCapitalOwner belongingCapitalOwner = belongingCapitalOwnerRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Belonging Capital Owner not found"));

        Long start;
        Long end;
        List<BelongingCapitalOwner> belongingCapitalOwners;

        if (belongingCapitalOwner.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = belongingCapitalOwner.getOrderingId();
            belongingCapitalOwners = belongingCapitalOwnerRepository.findInOrderingIdRange(start, end, belongingCapitalOwner.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (BelongingCapitalOwner b : belongingCapitalOwners) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = belongingCapitalOwner.getOrderingId();
            end = request.getOrderingId();
            belongingCapitalOwners = belongingCapitalOwnerRepository.findInOrderingIdRange(start, end, belongingCapitalOwner.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (BelongingCapitalOwner b : belongingCapitalOwners) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        belongingCapitalOwner.setOrderingId(request.getOrderingId());
        belongingCapitalOwners.add(belongingCapitalOwner);
        belongingCapitalOwnerRepository.saveAll(belongingCapitalOwners);
    }

    /**
     * Sorts all {@link BelongingCapitalOwner} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BELONGING_CAPITAL_OWNERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Belonging Capital Owner alphabetically");
        List<BelongingCapitalOwner> belongingCapitalOwners = belongingCapitalOwnerRepository.orderByName();
        long orderingId = 1;

        for (BelongingCapitalOwner b : belongingCapitalOwners) {
            b.setOrderingId(orderingId);
            orderingId++;
        }

        belongingCapitalOwnerRepository.saveAll(belongingCapitalOwners);
    }

    /**
     * Deletes {@link BelongingCapitalOwner} if the validations are passed.
     *
     * @param id ID of the {@link BelongingCapitalOwner}
     * @throws DomainEntityNotFoundException if {@link BelongingCapitalOwner} is not found.
     * @throws OperationNotAllowedException  if the {@link BelongingCapitalOwner} is already deleted.
     * @throws OperationNotAllowedException  if the {@link BelongingCapitalOwner} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BELONGING_CAPITAL_OWNERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Belonging Capital owner with ID: {}", id);
        BelongingCapitalOwner belongingCapitalOwner = belongingCapitalOwnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Belonging Capital owner not found"));

        if (belongingCapitalOwner.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        if (belongingCapitalOwnerRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        belongingCapitalOwner.setStatus(DELETED);
        belongingCapitalOwnerRepository.save(belongingCapitalOwner);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return belongingCapitalOwnerRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return belongingCapitalOwnerRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link BelongingCapitalOwner} at the end with the highest ordering ID.
     * If the request asks to save {@link BelongingCapitalOwner} as a default and a default {@link BelongingCapitalOwner} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link BelongingCapitalOwnerRequest}
     * @return {@link BelongingCapitalOwnerResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public BelongingCapitalOwnerResponse add(BelongingCapitalOwnerRequest request) {

        log.debug("Adding Belonging Capital Owner: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (belongingCapitalOwnerRepository.countBelongingCapitalOwnerByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-BelongingCapitalOwner with the same name already exists;");
            throw new OperationNotAllowedException("name-BelongingCapitalOwner with the same name already exists;");
        }

        Long lastSortOrder = belongingCapitalOwnerRepository.findLastOrderingId();
        BelongingCapitalOwner belongingCapitalOwner = new BelongingCapitalOwner(request);
        belongingCapitalOwner.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<BelongingCapitalOwner> currentDefaultBelongingCapitalOwnerOptional = belongingCapitalOwnerRepository.findByDefaultSelectionTrue();
            if (currentDefaultBelongingCapitalOwnerOptional.isPresent()) {
                BelongingCapitalOwner currentDefaultBelongingCapitalOwner = currentDefaultBelongingCapitalOwnerOptional.get();
                currentDefaultBelongingCapitalOwner.setDefaultSelection(false);
                belongingCapitalOwnerRepository.save(currentDefaultBelongingCapitalOwner);
            }
        }
        BelongingCapitalOwner belongingCapitalOwnerEntity = belongingCapitalOwnerRepository.save(belongingCapitalOwner);
        return new BelongingCapitalOwnerResponse(belongingCapitalOwnerEntity);
    }

    /**
     * Retrieves detailed information about {@link BelongingCapitalOwner} by ID
     *
     * @param id ID of {@link BelongingCapitalOwner}
     * @return {@link BelongingCapitalOwnerResponse}
     * @throws DomainEntityNotFoundException if no {@link BelongingCapitalOwner} was found with the provided ID.
     */
    public BelongingCapitalOwnerResponse view(Long id) {
        log.debug("Fetching Belonging Capital Owner with ID: {}", id);
        BelongingCapitalOwner belongingCapitalOwner = belongingCapitalOwnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-BelongingCapitalOwner not found"));
        return new BelongingCapitalOwnerResponse(belongingCapitalOwner);
    }

    /**
     * Edit the requested {@link BelongingCapitalOwner}.
     * If the request asks to save {@link BelongingCapitalOwner} as a default and a default {@link BelongingCapitalOwner} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link BelongingCapitalOwner}
     * @param request {@link BelongingCapitalOwnerRequest}
     * @return {@link BelongingCapitalOwnerResponse}
     * @throws DomainEntityNotFoundException if {@link BelongingCapitalOwner} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link BelongingCapitalOwner} is deleted.
     */
    @Transactional
    public BelongingCapitalOwnerResponse edit(Long id, BelongingCapitalOwnerRequest request) {
        log.debug("Editing Belonging Capital Owner: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        BelongingCapitalOwner belongingCapitalOwner = belongingCapitalOwnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Belonging Capital Owner not found"));

        if (belongingCapitalOwnerRepository.countBelongingCapitalOwnerByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !belongingCapitalOwner.getName().equals(request.getName().trim())) {
            log.error("name-BelongingCapitalOwner with the same name already exists;");
            throw new OperationNotAllowedException("name-BelongingCapitalOwner with the same name already exists;");
        }

        if (belongingCapitalOwner.getStatus().equals(DELETED)) {
            log.error("status-Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !belongingCapitalOwner.isDefaultSelection()) {
            Optional<BelongingCapitalOwner> currentDefaultBelongingOptional = belongingCapitalOwnerRepository.findByDefaultSelectionTrue();
            if (currentDefaultBelongingOptional.isPresent()) {
                BelongingCapitalOwner currentDefaultBelonging = currentDefaultBelongingOptional.get();
                currentDefaultBelonging.setDefaultSelection(false);
                belongingCapitalOwnerRepository.save(currentDefaultBelonging);
            }
        }
        belongingCapitalOwner.setDefaultSelection(request.getDefaultSelection());

        belongingCapitalOwner.setName(request.getName().trim());
        belongingCapitalOwner.setStatus(request.getStatus());
        return new BelongingCapitalOwnerResponse(belongingCapitalOwnerRepository.save(belongingCapitalOwner));
    }

    /**
     * Filters {@link BelongingCapitalOwner} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link BelongingCapitalOwner}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<BelongingCapitalOwnerResponse> Page&lt;BelongingCapitalOwnerResponse&gt;} containing detailed information
     */
    public Page<BelongingCapitalOwnerResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Belonging Capital Owner list with statuses: {}", request.toString());
        Page<BelongingCapitalOwner> page = belongingCapitalOwnerRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(BelongingCapitalOwnerResponse::new);
    }
}
