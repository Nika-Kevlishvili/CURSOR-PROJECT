package bg.energo.phoenix.service.nomenclature.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyDetailedResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.StringUtil;
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
public class CurrencyService implements NomenclatureBaseService {
    private final CurrencyRepository currencyRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CURRENCIES;
    }

    /**
     * Filters {@link Currency} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Currency}'s name.</li>
     *     <li>{@link Currency}'s abbreviation.</li>
     *     <li>{@link Currency}'s printName.</li>
     *     <li>{@link Currency}'s fullName.</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CURRENCIES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE}),
                    @PermissionMapping(context = PRODUCT_CONTRACTS, permissions = {
                            PRODUCT_CONTRACT_CREATE,
                            PRODUCT_CONTRACT_EDIT_READY,
                            PRODUCT_CONTRACT_EDIT_DRAFT,
                            PRODUCT_CONTRACT_EDIT_STATUS,
                            PRODUCT_CONTRACT_EDIT_LOCKED}),

                    @PermissionMapping(context = SERVICE_CONTRACTS, permissions = {
                            SERVICE_CONTRACT_CREATE,
                            SERVICE_CONTRACT_EDIT,
                            SERVICE_CONTRACT_EDIT_STATUSES,
                            SERVICE_CONTRACT_EDIT_READY,
                            SERVICE_CONTRACT_EDIT_DRAFT,
                            SERVICE_CONTRACT_EDIT_LOCKED})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {

        log.debug("Filtering currencies list with request: {}", request);
        return currencyRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Currency} item in the currency list to a specified position.
     * The method retrieves the {@link Currency} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Currency} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Currency} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CURRENCIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of currencies item with ID: {} to place {}", request.getId(), request.getOrderingId());

        Currency currency = currencyRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found"));

        Long start;
        Long end;
        List<Currency> currencies;

        if (currency.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = currency.getOrderingId();
            currencies = currencyRepository.findInOrderingIdRange(start, end, currency.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Currency b : currencies) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = currency.getOrderingId();
            end = request.getOrderingId();
            currencies = currencyRepository.findInOrderingIdRange(start, end, currency.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Currency b : currencies) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        currency.setOrderingId(request.getOrderingId());
        currencies.add(currency);
        currencyRepository.saveAll(currencies);
    }

    /**
     * Sorts all {@link Currency} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CURRENCIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the currencies alphabetically");
        List<Currency> currencies = currencyRepository.orderByName();
        long orderingId = 1;

        for (Currency m : currencies) {
            m.setOrderingId(orderingId);
            orderingId++;
        }

        currencyRepository.saveAll(currencies);
    }

    /**
     * Deletes {@link Currency} if the validations are passed.
     *
     * @param id ID of the {@link Currency}
     * @throws DomainEntityNotFoundException if {@link Currency} is not found.
     * @throws OperationNotAllowedException  if the {@link Currency} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Currency} is last main currency.
     * @throws OperationNotAllowedException  if the {@link Currency} is connected to price object.
     */

    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CURRENCIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing currency  with ID: {}", id);
        Currency currency = currencyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found"));

        if (currency.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        boolean hasActiveConnections = currencyRepository.hasActiveConnections(id);
        if (hasActiveConnections) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        Long mainCurrencyQuantity = currencyRepository.countByMainCurrencyAndStatus();
        if (mainCurrencyQuantity < 2 && currency.getMainCurrency() && currency.getStatus() == NomenclatureItemStatus.ACTIVE) {
            log.error("Cannot delete last main currency");
            throw new OperationNotAllowedException("Cannot delete last main currency");
        }

        List<Currency> parentCurrencies = currencyRepository.getAllByAltCurrencyId(currency.getId());
        if (!parentCurrencies.isEmpty()) {
            throw new OperationNotAllowedException("Cannot delete this item, it is connected to another currency");
        }
        currency.setStatus(DELETED);
        currencyRepository.save(currency);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return currencyRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return currencyRepository.findByIdIn(ids);
    }


    private void trimCurrencyRequest(CurrencyRequest request) {
        request.setName(request.getName() == null ? null : request.getName().trim());
        request.setPrintName(request.getPrintName() == null ? null : request.getPrintName().trim());
        request.setAbbreviation(request.getAbbreviation() == null ? null : request.getAbbreviation().trim());
        request.setFullName(request.getFullName() == null ? null : request.getFullName().trim());
    }

    /**
     * Adds {@link Currency} at the end with the highest ordering ID.
     *
     * @param request {@link CurrencyRequest}
     * @return {@link CurrencyResponse}
     * @throws ClientException              if there is another {@link Currency} with same start date and global enabled .
     * @throws ClientException              if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if there is another {@link Currency} with same name.
     * @throws OperationNotAllowedException if there already is two active {@link Currency}'s.
     */
    @Transactional
    public CurrencyResponse add(CurrencyRequest request) {
        log.debug("Adding Currency: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED;");
            throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
        }
        trimCurrencyRequest(request);

        Long activeCurrenciesQuantity = currencyRepository.countByActiveStatus();
        if (activeCurrenciesQuantity >= 2) {
            log.error("More Then 2 Active Currencies is not allowed;");
            throw new OperationNotAllowedException("More Then 2 Active Currencies is not allowed;");
        }

        if (currencyRepository.countCurrencyByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Currency with the same name already exists;");
            throw new OperationNotAllowedException("name-Currency with the same name already exists;");
        }

        if (request.getMainCurrencyStartDate() != null) {
            Optional<Currency> withSameStartDateAndActiveStatus = currencyRepository.findCurrencyByMainCurrencyStartDateAndMainCurrencyAndStatuses(request.getMainCurrencyStartDate(), request.getMainCurrency(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
            if (withSameStartDateAndActiveStatus.isPresent()) {
                log.error("mainCurrencyStartDate-Currency with the same start date and main enabled already exists;");
                throw new OperationNotAllowedException("mainCurrencyStartDate-Currency with the same main currency start date and main enabled already exists;");
            }
        }

        Long topId = currencyRepository.findLastOrderingId();
        Currency currency = new Currency(request);
        currency.setOrderingId(topId == null ? 1 : topId + 1);

        if (request.getAltCurrencyId() != null) {
            Currency altCurrency = currencyRepository
                    .findById(request.getAltCurrencyId())
                    .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

            if (altCurrency.getStatus().equals(DELETED)) {
                log.error("altCurrencyId-Cannot add  DELETED alt Currency");
                throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
            }
            currency.setAltCurrency(altCurrency);
            currency.setAltCurrencyExchangeRate(request.getAltCurrencyExchangeRate());
        }


        if (request.getDefaultSelection()) {
            Optional<Currency> currentDefaultCurrencyOptional = currencyRepository.findByDefaultSelectionTrue();
            if (currentDefaultCurrencyOptional.isPresent()) {
                Currency currentDefaultCurrency = currentDefaultCurrencyOptional.get();
                currentDefaultCurrency.setDefaultSelection(false);
                currencyRepository.save(currentDefaultCurrency);
            }
        }

        return new CurrencyResponse(currencyRepository.save(currency));
    }


    /**
     * Edit the requested {@link Currency}.
     *
     * @param id      ID of {@link Currency}
     * @param request {@link CurrencyRequest}
     * @return {@link CurrencyResponse}
     * @throws DomainEntityNotFoundException if {@link Currency} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Currency} is deleted.
     * @throws OperationNotAllowedException  if there is another {@link Currency} with same start date and global enabled .
     * @throws OperationNotAllowedException  if there is another {@link Currency} with same name.
     * @throws OperationNotAllowedException  if there already is two active {@link Currency}'s.
     */
    @Transactional
    public CurrencyResponse edit(Long id, CurrencyRequest request) {
        log.debug("Editing Currency: {}, with ID: {}", request.toString(), id);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
        }
        trimCurrencyRequest(request);

        if (request.getMainCurrencyStartDate() != null) {
            Optional<Currency> withSameStartDateAndActiveStatus = currencyRepository.findCurrencyByMainCurrencyStartDateAndMainCurrencyAndStatuses(request.getMainCurrencyStartDate(), request.getMainCurrency(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
            if (withSameStartDateAndActiveStatus.isPresent() && !withSameStartDateAndActiveStatus.get().getId().equals(id)) {
                log.error("mainCurrencyStartDate-Currency with the same main currency start date and main enabled already exists;");
                throw new OperationNotAllowedException("mainCurrencyStartDate-Currency with the same main currency start date and main enabled already exists;");
            }
        }

        Currency currency = currencyRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (currencyRepository.countCurrencyByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !currency.getName().equals(request.getName().trim())) {
            log.error("name-Currency with the same name already exists;");
            throw new OperationNotAllowedException("name-Currency with the same name already exists;");
        }

        Long activeCurrenciesQuantity = currencyRepository.countByActiveStatus();
        if (activeCurrenciesQuantity >= 2 && request.getStatus() != DELETED && currency.getStatus() == DELETED) {
            log.error("More Then 2 Active Currencies is not allowed;");
            throw new OperationNotAllowedException("More Then 2 Active Currencies is not allowed;");
        }

        if (currency.getStatus().equals(DELETED)) {
            log.error("status-Cannot edit DELETED item;");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item;");
        }

        if (request.getAltCurrencyId() != null) {
            Currency altCurrency = currencyRepository
                    .findById(request.getAltCurrencyId())
                    .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));
            if (currency.getAltCurrency() != null) {
                if (!currency.getAltCurrency().getId().equals(altCurrency.getId())) {
                    if (altCurrency.getStatus().equals(DELETED)) {
                        log.error("altCurrencyId-Cannot add Deleted Alt Currency;");
                        throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
                    }
                }
            } else {
                if (altCurrency.getStatus().equals(DELETED)) {
                    log.error("altCurrencyId-Cannot add Deleted Alt Currency;");
                    throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
                }
            }
            currency.setAltCurrency(altCurrency);
        } else {
            currency.setAltCurrency(null);
        }

        if (request.getDefaultSelection() && !currency.isDefaultSelection()) {
            Optional<Currency> currentDefaultCurrencyOptional = currencyRepository.findByDefaultSelectionTrue();
            if (currentDefaultCurrencyOptional.isPresent()) {
                Currency currentDefaultCurrency = currentDefaultCurrencyOptional.get();
                currentDefaultCurrency.setDefaultSelection(false);
                currencyRepository.save(currentDefaultCurrency);
            }
        }
        currency.setDefaultSelection(request.getDefaultSelection());

        currency.setName(request.getName());
        currency.setPrintName(request.getPrintName());
        currency.setAbbreviation(request.getAbbreviation());
        currency.setFullName(request.getFullName());
        currency.setAltCurrencyExchangeRate(request.getAltCurrencyExchangeRate());
        currency.setMainCurrency(request.getMainCurrency());
        currency.setMainCurrencyStartDate(request.getMainCurrency() ? request.getMainCurrencyStartDate() : null);
        currency.setStatus(request.getStatus());
        return new CurrencyResponse(currencyRepository.save(currency));
    }

    /**
     * Filters {@link Currency} against the provided {@link CurrencyFilterRequest}:
     * If regionId is provided in {@link CurrencyFilterRequest}, only those items will be returned which belong to the requested {@link Currency}.
     * If excludedItemId is provided in {@link CurrencyFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link CurrencyFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Currency}'s name.</li>
     *     <li>{@link Currency}'s abbreviation.</li>
     *     <li>{@link Currency}'s printName.</li>
     *     <li>{@link Currency}'s fullName.</li>
     * </ul>
     *
     * @param request {@link CurrencyFilterRequest}
     * @return {@link Page<CurrencyResponse> Page&lt;CurrencyResponse&gt;} containing detailed information
     */
    public Page<CurrencyResponse> filter(CurrencyFilterRequest request) {
        log.debug("Fetching currencies list with request: {}", request.toString());
        Page<Currency> page = currencyRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(request.getPrompt())),
                        request.getStatuses(),
                        request.getAltCurrencyId(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()));
        return page.map(CurrencyResponse::new);
    }

    /**
     * Retrieves detailed information about {@link Currency} by ID
     *
     * @param id ID of {@link Currency}
     * @return {@link CurrencyResponse}
     * @throws DomainEntityNotFoundException if no {@link Currency} was found with the provided ID.
     */
    public CurrencyResponse view(Long id) {
        log.debug("Fetching Currency with ID: {}", id);
        Currency currency = currencyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found, ID: " + id));
        return new CurrencyResponse(currency);
    }

    /**
     * Retrieves extended information about {@link Currency} including information about its alternative {@link Currency}
     *
     * @param id ID of {@link Currency}
     * @return {@link CurrencyDetailedResponse}
     * @throws DomainEntityNotFoundException if no {@link Currency} was found with the provided ID.
     */
    public CurrencyDetailedResponse detailedView(Long id) {
        log.debug("Fetching detailed currency with ID: {}", id);
        Currency currency = currencyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found, ID: " + id));
        return new CurrencyDetailedResponse(currency);
    }


}
