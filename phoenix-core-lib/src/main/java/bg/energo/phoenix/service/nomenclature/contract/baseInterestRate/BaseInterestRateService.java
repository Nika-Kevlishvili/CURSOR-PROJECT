package bg.energo.phoenix.service.nomenclature.contract.baseInterestRate;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.BaseInterestRate;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.BaseInterestRateRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.BaseInterestRateResponse;
import bg.energo.phoenix.repository.nomenclature.contract.BaseInterestRateRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.BASE_INTEREST_RATES;
import static bg.energo.phoenix.permissions.PermissionContextEnum.INTEREST_RATES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseInterestRateService implements NomenclatureBaseService {

    private final DateTimeFormatter dateFromFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BaseInterestRateRepository baseInterestRateRepository;
    private final BaseInterestRateMapper mapper;


    /**
     * @return {@link Nomenclature#BASE_INTEREST_RATES}
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.BASE_INTEREST_RATES;
    }


    /**
     * Filters the entities by the given request criteria.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching entity will be excluded from the result.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the search will be performed in {@link BaseInterestRate}'s dateFrom and percentageRate fields.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest} containing the filter criteria
     * @return page of {@link BaseInterestRateResponse} containing the filtered entities
     */
    public Page<BaseInterestRateResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering base interest rates with request: {}", request);
        Page<BaseInterestRate> page = baseInterestRateRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(mapper::responseFromEntity);
    }


    /**
     * Filters items against the provided {@link NomenclatureItemsBaseFilterRequest} criteria.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item will be excluded from the result.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the search will be performed in {@link BaseInterestRate}'s dateFrom and percentageRate fields.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest} containing the filter criteria
     * @return {@link Page<NomenclatureResponse>} containing the filtered items
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BASE_INTEREST_RATES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = INTEREST_RATES, permissions = {INTEREST_RATES_CREATE, INTEREST_RATES_EDIT})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering base interest rates with request: {}", request);
        Page<BaseInterestRate> baseInterestRates = baseInterestRateRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return baseInterestRates.map(mapper::nomenclatureResponseFromEntity);
    }


    /**
     * Adds a new {@link BaseInterestRate} entity at the end with the highest ordering ID.
     * If the request asks to save {@link BaseInterestRate} as a default and a default {@link BaseInterestRate} already exists,
     * the existing default {@link BaseInterestRate} will be set to non-default and the new {@link BaseInterestRate} will be set as default.
     *
     * @param request {@link BaseInterestRateRequest} containing the data to be saved
     * @return {@link BaseInterestRateResponse} containing the saved entity
     */
    @Transactional
    public BaseInterestRateResponse add(BaseInterestRateRequest request) {
        log.debug("Adding base interest rate with request: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new IllegalArgumentsProvidedException("status-Cannot add item with status DELETED");
        }

        if (baseInterestRateRepository.existsByDateFromAndStatusInAndIdNullOrIdNot(request.getDateFrom(), List.of(ACTIVE, INACTIVE), null)) {
            log.error("dateFrom-[Date From] field should be unique;");
            throw new IllegalArgumentsProvidedException("dateFrom-[Date From] field should be unique;");
        }

        Long lastOrderingId = baseInterestRateRepository.findLastOrderingId();
        BaseInterestRate baseInterestRate = mapper.entityFromRequest(request);
        baseInterestRate.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        baseInterestRate.setName(
                "%s - %s".formatted(
                        dateFromFormatter.format(baseInterestRate.getDateFrom()),
                        baseInterestRate.getPercentageRate()
                )
        );

        assignDefaultSelectionWhenAdding(request, baseInterestRate);
        baseInterestRateRepository.saveAndFlush(baseInterestRate);
        return mapper.responseFromEntity(baseInterestRate);
    }


    /**
     * Sets the default selection flag for the given {@link BaseInterestRate} based on the provided request when adding.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link BaseInterestRate} is set to false,
     * and the given {@link BaseInterestRate} is set as the new default selection.
     *
     * @param request          the {@link BaseInterestRateRequest} containing the status and default selection flag to use when setting the default selection
     * @param baseInterestRate the {@link BaseInterestRate} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenAdding(BaseInterestRateRequest request, BaseInterestRate baseInterestRate) {
        if (request.getStatus().equals(INACTIVE)) {
            baseInterestRate.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<BaseInterestRate> currentDefaultEntityOptional = baseInterestRateRepository.findByDefaultSelectionTrue();
                if (currentDefaultEntityOptional.isPresent()) {
                    BaseInterestRate currentDefaultItem = currentDefaultEntityOptional.get();
                    currentDefaultItem.setDefaultSelection(false);
                    baseInterestRateRepository.save(currentDefaultItem);
                }
            }
            baseInterestRate.setDefaultSelection(request.getDefaultSelection());
        }
    }


    /**
     * Changes the ordering of a {@link BaseInterestRate} item in the list to a specified position.
     * The method retrieves the {@link BaseInterestRate} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link BaseInterestRate} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link BaseInterestRate} item with the given ID is found
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BASE_INTEREST_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    @Transactional
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in base interest rates", request.getId());

        BaseInterestRate interestRate = baseInterestRateRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Base interest rate not found by ID: %s;".formatted(request.getId())));

        Long start;
        Long end;
        List<BaseInterestRate> interestRates;

        if (interestRate.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = interestRate.getOrderingId();
            interestRates = baseInterestRateRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            interestRate.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (BaseInterestRate ut : interestRates) {
                ut.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = interestRate.getOrderingId();
            end = request.getOrderingId();
            interestRates = baseInterestRateRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            interestRate.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (BaseInterestRate ir : interestRates) {
                ir.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        interestRate.setOrderingId(request.getOrderingId());
        interestRates.add(interestRate);
        baseInterestRateRepository.saveAll(interestRates);
    }


    /**
     * Sorts all entities by date from in ascending order not taking status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BASE_INTEREST_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the base interest rates by date from");

        List<BaseInterestRate> interestRates = baseInterestRateRepository.orderByDateFrom();
        long orderingId = 1;

        for (BaseInterestRate bir : interestRates) {
            bir.setOrderingId(orderingId);
            orderingId++;
        }

        baseInterestRateRepository.saveAll(interestRates);
    }


    /**
     * Sets DELETED status to {@link BaseInterestRate} entity if the validations are passed.
     *
     * @param id ID of the {@link BaseInterestRate} to be deleted.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = BASE_INTEREST_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Setting deleted status to base interest rate with ID: {}", id);

        BaseInterestRate baseInterestRate = baseInterestRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Base interest rate with ID: %s not found;".formatted(id)));

        if (baseInterestRate.getStatus().equals(DELETED)) {
            log.error("Base interest rate with ID {} is already deleted;", id);
            throw new OperationNotAllowedException("id-Base interest rate with ID [%s] is already deleted;".formatted(id));
        }

        baseInterestRate.setStatus(DELETED);
        baseInterestRateRepository.save(baseInterestRate);
    }


    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return baseInterestRateRepository.existsByIdAndStatusIn(id, statuses);
    }


    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return baseInterestRateRepository.findByIdIn(ids);
    }


    /**
     * Retrieves detailed information about {@link BaseInterestRate} by ID.
     *
     * @param id ID of the {@link BaseInterestRate} to be fetched.
     * @return {@link BaseInterestRateResponse} object
     */
    public BaseInterestRateResponse view(Long id) {
        log.debug("Fetching base interest rate with ID: {}", id);
        BaseInterestRate baseInterestRate = baseInterestRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Base interest rate with ID: %s not found;".formatted(id)));
        return mapper.responseFromEntity(baseInterestRate);
    }


    /**
     * Edits {@link BaseInterestRate} entity if the validations are passed.
     * If the request asks to save {@link BaseInterestRate} as a default and a default {@link BaseInterestRate} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * If the status inside request is {@link NomenclatureItemStatus#INACTIVE}, default selection will be set false,
     * no matter the requested default selection.
     *
     * @param id      ID of the {@link BaseInterestRate} to be edited.
     * @param request {@link BaseInterestRateRequest} object containing the new data.
     * @return {@link BaseInterestRateResponse} object containing the updated data.
     */
    @Transactional
    public BaseInterestRateResponse edit(Long id, BaseInterestRateRequest request) {
        log.debug("Updating base interest rate with ID: {} and request: {}", id, request);

        if (request.getStatus().equals(DELETED)) {
            log.error("id-You can't set DELETED status to nomenclature item;");
            throw new OperationNotAllowedException("id-You can't set DELETED status to nomenclature item;");
        }

        BaseInterestRate baseInterestRate = baseInterestRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Base interest rate with ID: %s not found;".formatted(id)));

        if (baseInterestRate.getStatus().equals(DELETED)) {
            log.error("id-Editing an already deleted nomenclature item is not allowed;");
            throw new OperationNotAllowedException("id-Editing a deleted nomenclature item with ID %s is not allowed;".formatted(baseInterestRate.getId()));
        }

        // if the dateFrom is changed, validate if the new date is unique
        if (baseInterestRateRepository.existsByDateFromAndStatusInAndIdNullOrIdNot(request.getDateFrom(), List.of(ACTIVE, INACTIVE), baseInterestRate.getId())) {
            log.debug("id-Base interest rate with date %s already exists;".formatted(request.getDateFrom().toString()));
            throw new OperationNotAllowedException("id-Base interest rate with date %s already exists;".formatted(request.getDateFrom().toString()));
        }

        assignDefaultSelectionWhenEditing(request, baseInterestRate);

        baseInterestRate.setName(
                "%s - %s".formatted(
                        dateFromFormatter.format(request.getDateFrom()),
                        request.getPercentageRate()
                )
        );
        baseInterestRate.setPercentageRate(request.getPercentageRate());
        baseInterestRate.setStatus(request.getStatus());
        baseInterestRateRepository.save(baseInterestRate);

        return mapper.responseFromEntity(baseInterestRate);
    }


    /**
     * Sets the default selection flag for the given {@link BaseInterestRate} based on the provided request when editing.
     * If the request's status is {@link NomenclatureItemStatus#INACTIVE}, the default selection flag is set to false.
     * If the request's default selection flag is true, then any existing default {@link BaseInterestRate} is set to false,
     * and the given {@link BaseInterestRate} is set as the new default selection.
     *
     * @param request          the {@link BaseInterestRateRequest} containing the status and default selection flag to use when setting the default selection
     * @param baseInterestRate the {@link BaseInterestRate} to set the default selection flag on
     */
    private void assignDefaultSelectionWhenEditing(BaseInterestRateRequest request, BaseInterestRate baseInterestRate) {
        if (request.getStatus().equals(INACTIVE)) {
            baseInterestRate.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!baseInterestRate.isDefaultSelection()) {
                    Optional<BaseInterestRate> currentDefaultEntityOptional = baseInterestRateRepository.findByDefaultSelectionTrue();
                    if (currentDefaultEntityOptional.isPresent()) {
                        BaseInterestRate currentDefaultEntity = currentDefaultEntityOptional.get();
                        currentDefaultEntity.setDefaultSelection(false);
                        baseInterestRateRepository.save(currentDefaultEntity);
                    }
                }
            }
            baseInterestRate.setDefaultSelection(request.getDefaultSelection());
        }
    }
}
