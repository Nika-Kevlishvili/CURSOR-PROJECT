package bg.energo.phoenix.service.nomenclature.address;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.address.Country;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.address.CountryRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.address.CountryResponse;
import bg.energo.phoenix.repository.nomenclature.address.CountryRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.COUNTRIES;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryService implements NomenclatureBaseService {
    private final CountryRepository countryRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.COUNTRIES;
    }

    /**
     * Filters the list of countries based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Country}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of CountryResponse objects containing the filtered list of countries.
     */
    public Page<CountryResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering countries list with request: {}", request.toString());
        Page<Country> page = countryRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt())),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(CountryResponse::new);
    }

    /**
     * Filters {@link Country} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Country}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COUNTRIES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering countries nomenclature with request: {}", request.toString());
        return countryRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link Country} at the end with the highest ordering ID.
     * If the request asks to save {@link Country} as a default and a default {@link Country} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link CountryRequest}
     * @return {@link CountryResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public CountryResponse add(CountryRequest request) {
        log.debug("Adding country: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (countryRepository.countCountryByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Country with the same name already exists;");
            throw new OperationNotAllowedException("name-Country with the same name already exists;");
        }

        Long lastSortOrder = countryRepository.findLastOrderingId();
        Country country = new Country(request);
        country.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Country> currentDefaultCountryOptional = countryRepository.findByDefaultSelectionTrue();
            if (currentDefaultCountryOptional.isPresent()) {
                Country currentDefaultCountry = currentDefaultCountryOptional.get();
                currentDefaultCountry.setDefaultSelection(false);
                countryRepository.save(currentDefaultCountry);
            }
        }
        Country countryEntity = countryRepository.save(country);
        return new CountryResponse(countryEntity);
    }

    /**
     * Retrieves detailed information about {@link Country} by ID
     *
     * @param id ID of {@link Country}
     * @return {@link CountryResponse}
     * @throws DomainEntityNotFoundException if no {@link Country} was found with the provided ID.
     */
    public CountryResponse view(Long id) {
        log.debug("Fetching country with ID: {}", id);
        Country country = countryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country not found, ID: " + id));
        return new CountryResponse(country);
    }

    /**
     * Edits the {@link Country}.
     * If the request asks to save {@link Country} as a default and a default {@link Country} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Country}
     * @param request {@link CountryRequest}
     * @return {@link CountryResponse}
     * @throws DomainEntityNotFoundException if {@link Country} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Country} is deleted.
     */
    @Transactional
    public CountryResponse edit(Long id, CountryRequest request) {
        log.debug("Editing country: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Country country = countryRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (countryRepository.countCountryByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !country.getName().equals(request.getName().trim())) {
            log.error("name-Country with the same name already exists;");
            throw new OperationNotAllowedException("name-Country with the same name already exists;");
        }

        if (country.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !country.isDefaultSelection()) {
            Optional<Country> currentDefaultCountryOptional = countryRepository.findByDefaultSelectionTrue();
            if (currentDefaultCountryOptional.isPresent()) {
                Country currentDefaultCountry = currentDefaultCountryOptional.get();
                currentDefaultCountry.setDefaultSelection(false);
                countryRepository.save(currentDefaultCountry);
            }
        }
        country.setDefaultSelection(request.getDefaultSelection());

        country.setName(request.getName().trim());
        country.setStatus(request.getStatus());
        return new CountryResponse(countryRepository.save(country));
    }

    /**
     * Deletes {@link Country} if the validations are passed.
     *
     * @param id ID of the {@link Country}
     * @throws DomainEntityNotFoundException if {@link Country} is not found.
     * @throws OperationNotAllowedException  if the {@link Country} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Country} has active or inactive children.
     * @throws OperationNotAllowedException  if the {@link Country} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COUNTRIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing country with ID: {}", id);
        Country country = countryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country not found, ID: " + id));

        if (country.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        Long childRegionsCount = countryRepository
                .getRegionsCountByStatusAndCountryId(List.of(ACTIVE, INACTIVE), country.getId());
        if (childRegionsCount > 0) {
            log.error("Item has active or inactive children.");
            throw new OperationNotAllowedException("id-Item has active or inactive children.");
        }

        if (countryRepository.getActiveConnectionsCount(id) > 0) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        country.setStatus(DELETED);

        countryRepository.save(country);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return countryRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return countryRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link Country} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COUNTRIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the countries alphabetically");
        List<Country> countries = countryRepository.orderByName();
        long orderingId = 1;

        for (Country c : countries) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        countryRepository.saveAll(countries);
    }

    /**
     * Changes the ordering of a {@link Country} item in the countries list to a specified position.
     * The method retrieves the country item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the country item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Country} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COUNTRIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of country item with ID: {} in countries to place: {}", request.getId(), request.getOrderingId());

        Country country = countryRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Country> countries;

        if (country.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = country.getOrderingId();

            countries = countryRepository.findInOrderingIdRange(
                    start,
                    end,
                    country.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Country c : countries) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = country.getOrderingId();
            end = request.getOrderingId();

            countries = countryRepository.findInOrderingIdRange(
                    start,
                    end,
                    country.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Country c : countries) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        country.setOrderingId(request.getOrderingId());
        countries.add(country);
        countryRepository.saveAll(countries);
    }
}