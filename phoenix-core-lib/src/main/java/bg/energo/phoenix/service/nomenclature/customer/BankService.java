package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.BankRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
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
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.BANKS;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService implements NomenclatureBaseService {
    private final BankRepository bankRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.BANKS;
    }

    /**
     * Filters {@link Bank} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Bank}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BANKS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering banks list with statuses: {}", request);
        return bankRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Bank} item in the Bank list to a specified position.
     * The method retrieves the {@link Bank} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Bank} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Bank} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BANKS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in banks to top", request.getId());

        Bank bank = bankRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Bank not found"));

        Long start;
        Long end;
        List<Bank> banks;

        if (bank.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = bank.getOrderingId();
            banks = bankRepository.findInOrderingIdRange(start, end, bank.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Bank b : banks) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = bank.getOrderingId();
            end = request.getOrderingId();
            banks = bankRepository.findInOrderingIdRange(start, end, bank.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Bank b : banks) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        bank.setOrderingId(request.getOrderingId());
        banks.add(bank);
        bankRepository.saveAll(banks);
    }

    /**
     * Sorts all {@link Bank} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BANKS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the banks alphabetically");
        List<Bank> banks = bankRepository.orderByName();
        long orderingId = 1;

        for (Bank b : banks) {
            b.setOrderingId(orderingId);
            orderingId++;
        }

        bankRepository.saveAll(banks);
    }

    /**
     * Deletes {@link Bank} if the validations are passed.
     *
     * @param id ID of the {@link Bank}
     * @throws DomainEntityNotFoundException if {@link Bank} is not found.
     * @throws OperationNotAllowedException  if the {@link Bank} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Bank} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BANKS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing bank with ID: {}", id);
        Bank bank = bankRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Bank not found"));

        if (bank.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (bankRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        bank.setStatus(DELETED);
        bankRepository.save(bank);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return bankRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return bankRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link Bank} at the end with the highest ordering ID.
     * If the request asks to save {@link Bank} as a default and a default {@link Bank} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link BankRequest}
     * @return {@link BankResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public BankResponse add(BankRequest request) {
        log.debug("Adding bank: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (bankRepository.countBankByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Bank with the same name already exists;");
            throw new OperationNotAllowedException("name-Bank with the same name already exists;");
        }

        if(bankRepository.existsByBicAndStatusIn(request.getBic(), List.of(ACTIVE, INACTIVE))) {
            log.error("bic-Bank with the same bic already exists;");
            throw new OperationNotAllowedException("bic-Bank with the same bic already exists;");
        }

        Long lastSortOrder = bankRepository.findLastOrderingId();
        Bank bank = new Bank(request);
        bank.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Bank> bankDefaultBankOptional = bankRepository.findByDefaultSelectionTrue();
            if (bankDefaultBankOptional.isPresent()) {
                Bank currentDefaultBank = bankDefaultBankOptional.get();
                currentDefaultBank.setDefaultSelection(false);
                bankRepository.save(currentDefaultBank);
            }
        }
        Bank bankEntity = bankRepository.save(bank);
        return new BankResponse(bankEntity);
    }

    /**
     * Retrieves detailed information about {@link Bank} by ID
     *
     * @param id ID of {@link Bank}
     * @return {@link BankResponse}
     * @throws DomainEntityNotFoundException if no {@link Bank} was found with the provided ID.
     */
    public BankResponse view(Long id) {
        log.debug("Fetching Bank with ID: {}", id);
        Bank bank = bankRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Bank not found"));
        return new BankResponse(bank);
    }

    /**
     * Edit the requested {@link Bank}.
     * If the request asks to save {@link Bank} as a default and a default {@link Bank} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Bank}
     * @param request {@link BankRequest}
     * @return {@link BankResponse}
     * @throws DomainEntityNotFoundException if {@link Bank} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Bank} is deleted.
     */
    @Transactional
    public BankResponse edit(Long id, BankRequest request) {
        log.debug("Editing Bank: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Bank bank = bankRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Bank not found"));

        if (bankRepository.countBankByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !bank.getName().equals(request.getName().trim())) {
            log.error("name-Bank with the same name already exists;");
            throw new OperationNotAllowedException("name-Bank with the same name already exists;");
        }

        if (bank.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item");
        }

        if(!Objects.equals(bank.getBic(), request.getBic()) && bankRepository.existsByBicAndStatusIn(request.getBic(), List.of(ACTIVE, INACTIVE))) {
            log.error("bic-Bank with the same bic already exists;");
            throw new OperationNotAllowedException("bic-Bank with the same bic already exists;");
        }

        if (request.getDefaultSelection() && !bank.isDefaultSelection()) {
            Optional<Bank> currentDefaultBankOptional = bankRepository.findByDefaultSelectionTrue();
            if (currentDefaultBankOptional.isPresent()) {
                Bank currentDefaultBank = currentDefaultBankOptional.get();
                currentDefaultBank.setDefaultSelection(false);
                bankRepository.save(currentDefaultBank);
            }
        }
        bank.setDefaultSelection(request.getDefaultSelection());

        bank.setName(request.getName().trim());
        bank.setBic(request.getBic().trim());
        bank.setStatus(request.getStatus());
        return new BankResponse(bankRepository.save(bank));
    }

    /**
     * Filters {@link Bank} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Bank}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<BankResponse> Page&lt;BankResponse&gt;} containing detailed information
     */
    public Page<BankResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering banks list with: {}", request.toString());
        Page<Bank> page = bankRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        return page.map(BankResponse::new);
    }
}
