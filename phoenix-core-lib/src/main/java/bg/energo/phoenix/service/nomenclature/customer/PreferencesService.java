package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Preferences;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.PreferencesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.PreferencesResponse;
import bg.energo.phoenix.repository.nomenclature.customer.PreferencesRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PREFERENCES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class PreferencesService implements NomenclatureBaseService {
    private final PreferencesRepository preferencesRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PREFERENCES;
    }

    /**
     * Filters {@link Preferences} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Preferences}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFERENCES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Preference list with statuses: {}", request);
        return preferencesRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Preferences} item in the Preferences list to a specified position.
     * The method retrieves the {@link Preferences} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Preferences} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Preferences} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFERENCES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of Preference with request: {}", request);
        Preferences preference = preferencesRepository.findByIdAndStatuses(request.getId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new ClientException("id-Preference not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Long start;
        Long end;
        List<Preferences> preferences;

        if (preference.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = preference.getOrderingId();
            preferences = preferencesRepository.findInOrderingIdRange(start, end, preference.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Preferences p : preferences) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = preference.getOrderingId();
            end = request.getOrderingId();
            preferences = preferencesRepository.findInOrderingIdRange(start, end, preference.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Preferences p : preferences) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        preference.setOrderingId(request.getOrderingId());
        preferencesRepository.save(preference);
        preferencesRepository.saveAll(preferences);
    }

    /**
     * Sorts all {@link Preferences} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFERENCES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting Preference alphabetically");
        List<Preferences> preferences = preferencesRepository.orderByName();
        long tempOrderingId = 1;
        for (Preferences c : preferences) {
            c.setOrderingId(tempOrderingId);
            tempOrderingId += 1;
        }
        preferencesRepository.saveAll(preferences);
    }

    /**
     * Deletes {@link Preferences} if the validations are passed.
     *
     * @param id ID of the {@link Preferences}
     * @throws DomainEntityNotFoundException if {@link Preferences} is not found.
     * @throws OperationNotAllowedException  if the {@link Preferences} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Preferences} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFERENCES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Preference with id: {}", id);
        Preferences preference = preferencesRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Preference with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (preference.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (preferencesRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        preference.setStatus(DELETED);
        preferencesRepository.save(preference);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return preferencesRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return preferencesRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link Preferences} at the end with the highest ordering ID.
     * If the request asks to save {@link Preferences} as a default and a default {@link Preferences} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link Preferences}
     * @return {@link PreferencesResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public PreferencesResponse add(PreferencesRequest request) {
        log.debug("Adding Preference with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add Preference with status DELETED");
            throw new ClientException("status-Cannot add Preference with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (preferencesRepository.countPreferencesByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Preference with the same name already exists;");
            throw new OperationNotAllowedException("name-Preference with the same name already exists;");
        }

        Preferences preferences = new Preferences(request);
        Long lastSortOrder = preferencesRepository.findLastSortOrder();
        preferences.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Preferences> defaultSelection = preferencesRepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                Preferences pref = defaultSelection.get();
                pref.setIsDefault(false);
                preferencesRepository.save(defaultSelection.get());
            }
        }
        Preferences save = preferencesRepository.save(preferences);

        return new PreferencesResponse(save);
    }

    /**
     * Edit the requested {@link Preferences}.
     * If the request asks to save {@link Preferences} as a default and a default {@link Preferences} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Preferences}
     * @param request {@link PreferencesRequest}
     * @return {@link PreferencesResponse}
     * @throws DomainEntityNotFoundException if {@link Preferences} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Preferences} is deleted.
     */
    @Transactional
    public PreferencesResponse edit(Long id, PreferencesRequest request) {
        log.debug("Editing Preference with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot edit Preference with status DELETED");
            throw new ClientException("status-Cannot add Credit Rating with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Preferences preference = preferencesRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Preference with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (preferencesRepository.countPreferencesByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !preference.getName().equals(request.getName().trim())) {
            log.error("name-Preference with the same name already exists;");
            throw new OperationNotAllowedException("name-Preference with the same name already exists;");
        }


        if (preference.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("id-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !preference.getIsDefault()) {
            Optional<Preferences> defaultSelection = preferencesRepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                Preferences pref = defaultSelection.get();
                pref.setIsDefault(false);
                preferencesRepository.save(defaultSelection.get());
            }
        }
        preference.setIsDefault(request.getDefaultSelection());

        preference.setName(request.getName().trim());
        preference.setStatus(request.getStatus());
        return new PreferencesResponse(preferencesRepository.save(preference));
    }

    /**
     * Retrieves detailed information about {@link Preferences} by ID
     *
     * @param id ID of {@link Preferences}
     * @return {@link PreferencesResponse}
     * @throws DomainEntityNotFoundException if no {@link Preferences} was found with the provided ID.
     */
    public PreferencesResponse view(Long id) {
        log.debug("Viewing Preference with id: {}", id);
        Preferences preference = preferencesRepository.findByIdAndStatuses(id, List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new ClientException("id-Preference with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new PreferencesResponse(preference);
    }

    /**
     * Filters {@link Preferences} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Preferences}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<PreferencesResponse> Page&lt;PreferencesResponse&gt;} containing detailed information
     */
    public Page<PreferencesResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        return preferencesRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(PreferencesResponse::new);
    }
}
