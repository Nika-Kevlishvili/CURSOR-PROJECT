package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.UnwantedCustomerReason;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.UnwantedCustomerReasonRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.UnwantedCustomerReasonResponse;
import bg.energo.phoenix.repository.nomenclature.customer.UnwantedCustomerReasonRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.UC;
import static bg.energo.phoenix.permissions.PermissionContextEnum.UNWANTED_CUSTOMER_REASON;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnwantedCustomerReasonService implements NomenclatureBaseService {
    private final UnwantedCustomerReasonRepository unwantedCustomerReasonRepository;


    /**
     * Filters {@link UnwantedCustomerReason} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link UnwantedCustomerReason}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<UnwantedCustomerReasonResponse> Page&lt;UnwantedCustomerReasonResponse&gt;} containing detailed information
     */
    public Page<UnwantedCustomerReasonResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering UNWANTED_CUSTOMER_REASON list with request: {}", request.toString());
        Page<UnwantedCustomerReason> page = unwantedCustomerReasonRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(UnwantedCustomerReasonResponse::new);
    }


    /**
     * Filters {@link UnwantedCustomerReason} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link UnwantedCustomerReason}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = UNWANTED_CUSTOMER_REASON, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = UC, permissions = {
                            UC_VIEW_BASIC,
                            UC_VIEW_DELETED
                    }),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering UnwantedCustomerReasons list with statuses: {}", request);
        return unwantedCustomerReasonRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Adds {@link UnwantedCustomerReason} at the end with the highest ordering ID.
     * If the request asks to save {@link UnwantedCustomerReason} as a default and a default {@link UnwantedCustomerReason} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link UnwantedCustomerReason}
     * @return {@link UnwantedCustomerReasonResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public UnwantedCustomerReasonResponse add(UnwantedCustomerReasonRequest request) {
        log.debug("Adding Unwanted Customer Reason: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (unwantedCustomerReasonRepository.countUnwantedCustomerReasonByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-UnwantedCustomerReason with the same name already exists;");
            throw new OperationNotAllowedException("name-UnwantedCustomerReason with the same name already exists;");
        }

        Long lastSortOrder = unwantedCustomerReasonRepository.findLastOrderingId();
        UnwantedCustomerReason unwantedCustomerReason = new UnwantedCustomerReason(request);
        unwantedCustomerReason.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<UnwantedCustomerReason> currentDefaultUnwantedCustomerReasonOptional =
                    unwantedCustomerReasonRepository.findByDefaultSelectionTrue();
            if (currentDefaultUnwantedCustomerReasonOptional.isPresent()) {
                UnwantedCustomerReason currentDefaultUnwantedCustomerReason = currentDefaultUnwantedCustomerReasonOptional.get();
                currentDefaultUnwantedCustomerReason.setDefaultSelection(false);
                unwantedCustomerReasonRepository.save(currentDefaultUnwantedCustomerReason);
            }
        }
        UnwantedCustomerReason unwantedCustomerReasonEntity = unwantedCustomerReasonRepository.save(unwantedCustomerReason);
        return new UnwantedCustomerReasonResponse(unwantedCustomerReasonEntity);
    }


    /**
     * Retrieves detailed information about {@link UnwantedCustomerReason} by ID
     *
     * @param id ID of {@link UnwantedCustomerReason}
     * @return {@link UnwantedCustomerReasonResponse}
     * @throws DomainEntityNotFoundException if no {@link UnwantedCustomerReason} was found with the provided ID.
     */
    public UnwantedCustomerReasonResponse view(Long id) {
        log.debug("Fetching UnwantedCustomerReason with ID: {}", id);
        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new UnwantedCustomerReasonResponse(unwantedCustomerReason);
    }

    /**
     * Edit the requested {@link UnwantedCustomerReason}.
     * If the request asks to save {@link UnwantedCustomerReason} as a default and a default {@link UnwantedCustomerReason} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link UnwantedCustomerReason}
     * @param request {@link UnwantedCustomerReasonRequest}
     * @return {@link UnwantedCustomerReasonResponse}
     * @throws DomainEntityNotFoundException if {@link UnwantedCustomerReason} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link UnwantedCustomerReason} is deleted.
     */
    @Transactional
    public UnwantedCustomerReasonResponse edit(Long id, UnwantedCustomerReasonRequest request) {
        log.debug("Editing UnwantedCustomerReason: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (unwantedCustomerReasonRepository.countUnwantedCustomerReasonByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !unwantedCustomerReason.getName().equals(request.getName().trim())) {
            log.error("name-UnwantedCustomerReason with the same name already exists;");
            throw new OperationNotAllowedException("name-UnwantedCustomerReason with the same name already exists;");
        }

        if (unwantedCustomerReason.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !unwantedCustomerReason.isDefaultSelection()) {
            Optional<UnwantedCustomerReason> currentDefaultUnwantedCustomerReasonOptional =
                    unwantedCustomerReasonRepository.findByDefaultSelectionTrue();
            if (currentDefaultUnwantedCustomerReasonOptional.isPresent()) {
                UnwantedCustomerReason currentDefaultUnwantedCustomerReason =
                        currentDefaultUnwantedCustomerReasonOptional.get();
                currentDefaultUnwantedCustomerReason.setDefaultSelection(false);
                unwantedCustomerReasonRepository.save(currentDefaultUnwantedCustomerReason);
            }
        }
        unwantedCustomerReason.setDefaultSelection(request.getDefaultSelection());

        unwantedCustomerReason.setName(request.getName().trim());
        unwantedCustomerReason.setStatus(request.getStatus());
        return new UnwantedCustomerReasonResponse(unwantedCustomerReasonRepository.save(unwantedCustomerReason));
    }

    /**
     * Deletes {@link UnwantedCustomerReason} if the validations are passed.
     *
     * @param id ID of the {@link UnwantedCustomerReason}
     * @throws DomainEntityNotFoundException if {@link UnwantedCustomerReason} is not found.
     * @throws OperationNotAllowedException  if the {@link UnwantedCustomerReason} is already deleted.
     * @throws OperationNotAllowedException  if the {@link UnwantedCustomerReason} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = UNWANTED_CUSTOMER_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing unwantedCustomerReason with ID: {}", id);
        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (unwantedCustomerReason.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (unwantedCustomerReasonRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        unwantedCustomerReason.setStatus(DELETED);
        unwantedCustomerReasonRepository.save(unwantedCustomerReason);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return unwantedCustomerReasonRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return unwantedCustomerReasonRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link UnwantedCustomerReason} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = UNWANTED_CUSTOMER_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the unwantedCustomerReasons alphabetically");
        List<UnwantedCustomerReason> unwantedCustomerReasonList = unwantedCustomerReasonRepository.orderByName();
        long orderingId = 1;

        for (UnwantedCustomerReason c : unwantedCustomerReasonList) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        unwantedCustomerReasonRepository.saveAll(unwantedCustomerReasonList);
    }

    /**
     * Changes the ordering of a {@link UnwantedCustomerReason} item in the UnwantedCustomerReason list to a specified position.
     * The method retrieves the {@link UnwantedCustomerReason} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link UnwantedCustomerReason} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link UnwantedCustomerReason} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = UNWANTED_CUSTOMER_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in unwantedCustomerReason to top", request.getId());

        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<UnwantedCustomerReason> unwantedCustomerReasonList;

        if (unwantedCustomerReason.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = unwantedCustomerReason.getOrderingId();
            unwantedCustomerReasonList = unwantedCustomerReasonRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            unwantedCustomerReason.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (UnwantedCustomerReason c : unwantedCustomerReasonList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = unwantedCustomerReason.getOrderingId();
            end = request.getOrderingId();
            unwantedCustomerReasonList =
                    unwantedCustomerReasonRepository.findInOrderingIdRange(
                            start,
                            end,
                            unwantedCustomerReason.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (UnwantedCustomerReason c : unwantedCustomerReasonList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        unwantedCustomerReason.setOrderingId(request.getOrderingId());
        unwantedCustomerReasonRepository.save(unwantedCustomerReason);
        unwantedCustomerReasonRepository.saveAll(unwantedCustomerReasonList);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.UNWANTED_CUSTOMER_REASON;
    }
}
