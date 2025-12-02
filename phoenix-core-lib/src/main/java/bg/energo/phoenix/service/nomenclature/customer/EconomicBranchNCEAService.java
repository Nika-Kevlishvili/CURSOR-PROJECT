package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.EconomicBranchNCEA;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.EconomicBranchNCEARequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.EconomicBranchNCEAResponse;
import bg.energo.phoenix.repository.nomenclature.customer.EconomicBranchNCEARepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.ECONOMIC_BRANCH_NCEA;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EconomicBranchNCEAService implements NomenclatureBaseService {
    private final EconomicBranchNCEARepository branchNCEARepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ECONOMIC_BRANCH_NCEA;
    }

    /**
     * Filters {@link EconomicBranchNCEA} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link EconomicBranchNCEA}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_NCEA, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering EconomicBranchNCEA list with statuses: {}", request);
        return branchNCEARepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link EconomicBranchNCEA} item in the EconomicBranchNCEA list to a specified position.
     * The method retrieves the {@link EconomicBranchNCEA} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link EconomicBranchNCEA} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link EconomicBranchNCEA} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_NCEA, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of EconomicBranchNCEA with request: {}", request);
        EconomicBranchNCEA economicBranchNCEA = branchNCEARepository.findByIdAndStatuses(request.getId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new ClientException("id-EconomicBranchNCEA not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Long start;
        Long end;
        List<EconomicBranchNCEA> branchNCEAS;

        if (economicBranchNCEA.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = economicBranchNCEA.getOrderingId();
            branchNCEAS = branchNCEARepository.findInOrderingIdRange(start, end, economicBranchNCEA.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (EconomicBranchNCEA p : branchNCEAS) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = economicBranchNCEA.getOrderingId();
            end = request.getOrderingId();
            branchNCEAS = branchNCEARepository.findInOrderingIdRange(start, end, economicBranchNCEA.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (EconomicBranchNCEA p : branchNCEAS) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        economicBranchNCEA.setOrderingId(request.getOrderingId());
        branchNCEARepository.save(economicBranchNCEA);
        branchNCEARepository.saveAll(branchNCEAS);
    }

    /**
     * Sorts all {@link EconomicBranchNCEA} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_NCEA, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting EconomicBranchNCEA alphabetically");
        List<EconomicBranchNCEA> branchNCEAS = branchNCEARepository.orderByName();
        long tempOrderingId = 1;
        for (EconomicBranchNCEA c : branchNCEAS) {
            c.setOrderingId(tempOrderingId);
            tempOrderingId += 1;
        }
        branchNCEARepository.saveAll(branchNCEAS);
    }

    /**
     * Deletes {@link EconomicBranchNCEA} if the validations are passed.
     *
     * @param id ID of the {@link EconomicBranchNCEA}
     * @throws DomainEntityNotFoundException if {@link EconomicBranchNCEA} is not found.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchNCEA} is already deleted.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchNCEA} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_NCEA, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing EconomicBranchNCEA with id: {}", id);
        EconomicBranchNCEA branchNCEA = branchNCEARepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-EconomicBranchNCEA with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (branchNCEA.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (branchNCEARepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        branchNCEA.setStatus(DELETED);
        branchNCEARepository.save(branchNCEA);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return branchNCEARepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return branchNCEARepository.findByIdIn(ids);
    }

    /**
     * Adds {@link EconomicBranchNCEA} at the end with the highest ordering ID.
     * If the request asks to save {@link EconomicBranchNCEA} as a default and a default {@link EconomicBranchNCEA} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link EconomicBranchNCEA}
     * @return {@link EconomicBranchNCEAResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public EconomicBranchNCEAResponse add(EconomicBranchNCEARequest request) {
        log.debug("Adding EconomicBranchNCEA with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add EconomicBranchNCEA with status DELETED");
            throw new ClientException("status-Cannot add EconomicBranchNCEA with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (branchNCEARepository.countEconomicBranchNCEAByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-EconomicBranchNCEA with the same name already exists;");
            throw new OperationNotAllowedException("name-EconomicBranchNCEA with the same name already exists;");
        }

        EconomicBranchNCEA branchNCEA = new EconomicBranchNCEA(request);
        Long lastSortOrder = branchNCEARepository.findLastSortOrder();
        branchNCEA.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<EconomicBranchNCEA> defaultSelection = branchNCEARepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                EconomicBranchNCEA def = defaultSelection.get();
                def.setIsDefault(false);
                branchNCEARepository.save(defaultSelection.get());
            }
        }
        EconomicBranchNCEA save = branchNCEARepository.save(branchNCEA);

        return new EconomicBranchNCEAResponse(save);
    }

    /**
     * Edit the requested {@link EconomicBranchNCEA}.
     * If the request asks to save {@link EconomicBranchNCEA} as a default and a default {@link EconomicBranchNCEA} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link EconomicBranchNCEA}
     * @param request {@link EconomicBranchNCEARequest}
     * @return {@link EconomicBranchNCEAResponse}
     * @throws DomainEntityNotFoundException if {@link EconomicBranchNCEA} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchNCEA} is deleted.
     */
    @Transactional
    public EconomicBranchNCEAResponse edit(Long id, EconomicBranchNCEARequest request) {
        log.debug("Editing EconomicBranchNCEA with request: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add EconomicBranchNCEA with status DELETED");
            throw new ClientException("status-Cannot add EconomicBranchNCEA with status DELETED",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        EconomicBranchNCEA branchNCEA = branchNCEARepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-EconomicBranchNCEA with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (branchNCEARepository.countEconomicBranchNCEAByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !branchNCEA.getName().equals(request.getName().trim())) {
            log.error("name-EconomicBranchNCEA with the same name already exists;");
            throw new OperationNotAllowedException("name-EconomicBranchNCEA with the same name already exists;");
        }


        if (branchNCEA.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("id-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !branchNCEA.getIsDefault()) {
            Optional<EconomicBranchNCEA> defaultSelection = branchNCEARepository.findByDefaultSelection();
            if (defaultSelection.isPresent()) {
                EconomicBranchNCEA def = defaultSelection.get();
                def.setIsDefault(false);
                branchNCEARepository.save(defaultSelection.get());
            }
        }
        branchNCEA.setIsDefault(request.getDefaultSelection());

        branchNCEA.setName(request.getName().trim());
        branchNCEA.setStatus(request.getStatus());
        return new EconomicBranchNCEAResponse(branchNCEARepository.save(branchNCEA));
    }

    /**
     * Retrieves detailed information about {@link EconomicBranchNCEA} by ID
     *
     * @param id ID of {@link EconomicBranchNCEA}
     * @return {@link EconomicBranchNCEAResponse}
     * @throws DomainEntityNotFoundException if no {@link EconomicBranchNCEA} was found with the provided ID.
     */
    public EconomicBranchNCEAResponse view(Long id) {
        log.debug("Viewing EconomicBranchNCEA with id: {}", id);
        EconomicBranchNCEA branchNCEA = branchNCEARepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-EconomicBranchNCEA with id: " + id + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new EconomicBranchNCEAResponse(branchNCEA);
    }

    /**
     * Filters {@link EconomicBranchNCEA} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link EconomicBranchNCEA}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<EconomicBranchNCEAResponse> Page&lt;EconomicBranchNCEAResponse&gt;} containing detailed information
     */
    public Page<EconomicBranchNCEAResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Credit Rating with request: {}", request);
        return branchNCEARepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(EconomicBranchNCEAResponse::new);
    }
}
