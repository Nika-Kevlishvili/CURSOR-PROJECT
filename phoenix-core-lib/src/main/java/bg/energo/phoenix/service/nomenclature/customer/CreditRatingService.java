package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.CreditRating;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.CreditRatingRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.CreditRatingResponse;
import bg.energo.phoenix.repository.nomenclature.customer.CreditRatingRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.CREDIT_RATING;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditRatingService implements NomenclatureBaseService {

    private final CreditRatingRepository creditRatingRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CREDIT_RATING;
    }

    /**
     * Filters {@link CreditRating} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link CreditRating}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CREDIT_RATING, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Credit Rating list with statuses: {}", request);
        return creditRatingRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link CreditRating} item in the CreditRating list to a specified position.
     * The method retrieves the {@link CreditRating} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link CreditRating} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link CreditRating} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CREDIT_RATING, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of Credit Rating with request: {}", request);
        CreditRating creditRating = creditRatingRepository.findByIdAndStatuses(request.getId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new ClientException("id-Credit Rating with id: " + request.getId() + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Long start;
        Long end;
        List<CreditRating> creditRatings;

        if (creditRating.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = creditRating.getOrderingId();
            creditRatings = creditRatingRepository.findInOrderingIdRange(start, end, creditRating.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (CreditRating p : creditRatings) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = creditRating.getOrderingId();
            end = request.getOrderingId();
            creditRatings = creditRatingRepository.findInOrderingIdRange(start, end, creditRating.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (CreditRating p : creditRatings) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        creditRating.setOrderingId(request.getOrderingId());
        creditRatingRepository.save(creditRating);
        creditRatingRepository.saveAll(creditRatings);
    }

    /**
     * Sorts all {@link CreditRating} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CREDIT_RATING, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting Credit Rating alphabetically");
        List<CreditRating> creditRatings = creditRatingRepository.orderByName();
        long tempOrderingId = 1;
        for (CreditRating c : creditRatings) {
            c.setOrderingId(tempOrderingId);
            tempOrderingId += 1;
        }
        creditRatingRepository.saveAll(creditRatings);
    }

    /**
     * Deletes {@link CreditRating} if the validations are passed.
     *
     * @param id ID of the {@link CreditRating}
     * @throws DomainEntityNotFoundException if {@link CreditRating} is not found.
     * @throws OperationNotAllowedException  if the {@link CreditRating} is already deleted.
     * @throws OperationNotAllowedException  if the {@link CreditRating} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CREDIT_RATING, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Credit Rating with id: {}", id);
        CreditRating creditRating = creditRatingRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Credit Rating with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (creditRating.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (creditRatingRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        creditRating.setStatus(DELETED);
        creditRatingRepository.save(creditRating);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return creditRatingRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return creditRatingRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link CreditRating} at the end with the highest ordering ID.
     * If the request asks to save {@link CreditRating} as a default and a default {@link CreditRating} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link CreditRating}
     * @return {@link CreditRatingResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public CreditRatingResponse add(CreditRatingRequest request) {
        log.debug("Adding Credit Rating with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add Credit Rating with status DELETED");
            throw new ClientException("status-Cannot add Credit Rating with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (creditRatingRepository.countCreditRatingByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-CreditRating with the same name already exists;");
            throw new OperationNotAllowedException("name-CreditRating with the same name already exists;");
        }

        CreditRating creditRating = new CreditRating(request);
        Long lastSortOrder = creditRatingRepository.findLastSortOrder();
        creditRating.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<CreditRating> defaultSelection = creditRatingRepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                CreditRating rating = defaultSelection.get();
                rating.setIsDefault(false);
                creditRatingRepository.save(defaultSelection.get());
            }
        }
        CreditRating save = creditRatingRepository.save(creditRating);

        return new CreditRatingResponse(save);
    }

    /**
     * Edit the requested {@link CreditRating}.
     * If the request asks to save {@link CreditRating} as a default and a default {@link CreditRating} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link CreditRating}
     * @param request {@link CreditRatingRequest}
     * @return {@link CreditRatingResponse}
     * @throws DomainEntityNotFoundException if {@link CreditRating} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link CreditRating} is deleted.
     */
    @Transactional
    public CreditRatingResponse edit(Long id, CreditRatingRequest request) {
        log.debug("Editing Credit Rating with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot edit Credit Rating with status DELETED");
            throw new ClientException("status-Cannot add Credit Rating with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        CreditRating creditRating = creditRatingRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Credit Rating with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (creditRatingRepository.countCreditRatingByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !creditRating.getName().equals(request.getName().trim())) {
            log.error("name-CreditRating with the same name already exists;");
            throw new OperationNotAllowedException("name-CreditRating with the same name already exists;");
        }

        if (creditRating.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("id-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !creditRating.getIsDefault()) {
            Optional<CreditRating> defaultSelection = creditRatingRepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                CreditRating rating = defaultSelection.get();
                rating.setIsDefault(false);
                creditRatingRepository.save(defaultSelection.get());
            }
        }
        creditRating.setIsDefault(request.getDefaultSelection());

        creditRating.setName(request.getName().trim());
        creditRating.setStatus(request.getStatus());
        return new CreditRatingResponse(creditRatingRepository.save(creditRating));
    }

    /**
     * Retrieves detailed information about {@link CreditRating} by ID
     *
     * @param id ID of {@link CreditRating}
     * @return {@link CreditRatingResponse}
     * @throws DomainEntityNotFoundException if no {@link CreditRating} was found with the provided ID.
     */
    public CreditRatingResponse view(Long id) {
        log.debug("Viewing Credit Rating with id: {}", id);
        CreditRating creditRating = creditRatingRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Credit Rating with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new CreditRatingResponse(creditRating);
    }

    /**
     * Filters {@link CreditRating} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link CreditRating}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<CreditRatingResponse> Page&lt;CreditRatingResponse&gt;} containing detailed information
     */
    public Page<CreditRatingResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Credit Rating with request: {}", request);
        return creditRatingRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(CreditRatingResponse::new);
    }
}
