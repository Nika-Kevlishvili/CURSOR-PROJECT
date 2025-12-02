package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.AccountManagerTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.AccountManagerTypeResponse;
import bg.energo.phoenix.repository.nomenclature.customer.AccountManagerTypeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.ACCOUNT_MANAGER_TYPES;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountManagerTypeService implements NomenclatureBaseService {

    private final AccountManagerTypeRepository accountManagerTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ACCOUNT_MANAGER_TYPES;
    }

    /**
     * Filters {@link AccountManagerType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link AccountManagerType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACCOUNT_MANAGER_TYPES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering AccountManagerType list with statuses: {}", request);
        return accountManagerTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link AccountManagerType} item in the AccountManagerTypes list to a specified position.
     * The method retrieves the {@link AccountManagerType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link AccountManagerType} item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link AccountManagerType} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACCOUNT_MANAGER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in account manager type to top", request.getId());

        AccountManagerType accountManagerType = accountManagerTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Account Manager Type not found"));

        Long start;
        Long end;
        List<AccountManagerType> accountManagerTypes;

        if (accountManagerType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = accountManagerType.getOrderingId();
            accountManagerTypes = accountManagerTypeRepository.findInOrderingIdRange(start, end, accountManagerType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (AccountManagerType a : accountManagerTypes) {
                a.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = accountManagerType.getOrderingId();
            end = request.getOrderingId();
            accountManagerTypes = accountManagerTypeRepository.findInOrderingIdRange(start, end, accountManagerType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() -1;
            for (AccountManagerType a : accountManagerTypes) {
                a.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        accountManagerType.setOrderingId(request.getOrderingId());
        accountManagerTypes.add(accountManagerType);
        accountManagerTypeRepository.saveAll(accountManagerTypes);
    }

    /**
     * Sorts all {@link AccountManagerType} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACCOUNT_MANAGER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the account manager types alphabetically");
        List<AccountManagerType> accountManagerTypes = accountManagerTypeRepository.orderByName();
        long orderingId = 1;

        for (AccountManagerType a : accountManagerTypes) {
            a.setOrderingId(orderingId);
            orderingId++;
        }

        accountManagerTypeRepository.saveAll(accountManagerTypes);
    }

    /**
     * Deletes {@link AccountManagerType} if the validations are passed.
     *
     * @param id ID of the {@link AccountManagerType}
     * @throws DomainEntityNotFoundException if {@link AccountManagerType} is not found.
     * @throws OperationNotAllowedException if the {@link AccountManagerType} is already deleted.
     * @throws OperationNotAllowedException if the {@link AccountManagerType} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ACCOUNT_MANAGER_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Account Manager Type with ID: {}", id);
        AccountManagerType accountManagerType = accountManagerTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Account Manager Type not found"));

        if (accountManagerType.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        if (accountManagerTypeRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        accountManagerType.setStatus(DELETED);
        accountManagerTypeRepository.save(accountManagerType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return accountManagerTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return accountManagerTypeRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link AccountManagerType} at the end with the highest ordering ID.
     * If the request asks to save {@link AccountManagerType} as a default and a default {@link AccountManagerType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link AccountManagerTypeRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link AccountManagerTypeResponse}
     */
    @Transactional
    public AccountManagerTypeResponse add(AccountManagerTypeRequest request) {
        log.debug("Adding AccountManagerType: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (accountManagerTypeRepository.countAccountManagerTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-AccountManagerType with the same name already exists;");
            throw new OperationNotAllowedException("name-AccountManagerType with the same name already exists;");
        }

        Long lastSortOrder = accountManagerTypeRepository.findLastOrderingId();
        AccountManagerType accountManagerType = new AccountManagerType(request);
        accountManagerType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<AccountManagerType> currentDefaultAccountManagerTypeOptional = accountManagerTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultAccountManagerTypeOptional.isPresent()) {
                AccountManagerType currentDefaultAccountManagerType = currentDefaultAccountManagerTypeOptional.get();
                currentDefaultAccountManagerType.setDefaultSelection(false);
                accountManagerTypeRepository.save(currentDefaultAccountManagerType);
            }
        }
        AccountManagerType accountManagerTypeEntity = accountManagerTypeRepository.save(accountManagerType);
        return new AccountManagerTypeResponse(accountManagerTypeEntity);
    }

    /**
     * Retrieves detailed information about {@link AccountManagerType} by ID
     *
     * @param id ID of {@link AccountManagerType}
     * @return {@link AccountManagerTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link AccountManagerType} was found with the provided ID.
     */
    public AccountManagerTypeResponse view(Long id) {
        log.debug("Fetching Account Manager type with ID: {}", id);
        AccountManagerType accountManagerType = accountManagerTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Account Manager type not found"));
        return new AccountManagerTypeResponse(accountManagerType);
    }

    /**
     * Edit the requested {@link AccountManagerType}.
     * If the request asks to save {@link AccountManagerType} as a default and a default {@link AccountManagerType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link AccountManagerType}
     * @param request {@link AccountManagerTypeRequest}
     * @throws DomainEntityNotFoundException if {@link AccountManagerType} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link AccountManagerType} is deleted.
     * @return {@link AccountManagerTypeResponse}
     */
    @Transactional
    public AccountManagerTypeResponse edit(Long id, AccountManagerTypeRequest request) {
        log.debug("Editing Account Manager Type: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        AccountManagerType accountManagerType = accountManagerTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Account Manager Type not found"));

        if (accountManagerTypeRepository.countAccountManagerTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !accountManagerType.getName().equals(request.getName().trim())) {
            log.error("name-AccountManagerType with the same name already exists;");
            throw new OperationNotAllowedException("name-AccountManagerType with the same name already exists;");
        }

        if (accountManagerType.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !accountManagerType.isDefaultSelection()) {
            Optional<AccountManagerType> currentDefaultAccountManagerTypeOptional = accountManagerTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultAccountManagerTypeOptional.isPresent()) {
                AccountManagerType currentDefaultAccountManagerType = currentDefaultAccountManagerTypeOptional.get();
                currentDefaultAccountManagerType.setDefaultSelection(false);
                accountManagerTypeRepository.save(currentDefaultAccountManagerType);
            }
        }
        accountManagerType.setDefaultSelection(request.getDefaultSelection());

        accountManagerType.setName(request.getName().trim());
        accountManagerType.setStatus(request.getStatus());
        return new AccountManagerTypeResponse(accountManagerTypeRepository.save(accountManagerType));
    }

    /**
     * Filters {@link AccountManagerType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link AccountManagerType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<AccountManagerTypeResponse> Page&lt;AccountManagerTypeResponse&gt;} containing detailed information
     */
    public Page<AccountManagerTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering AccountManagerType list with statuses: {}", request.toString());
        Page<AccountManagerType> page = accountManagerTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(AccountManagerTypeResponse::new);
    }
}
