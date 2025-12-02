package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.EconomicBranchCI;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.EconomicBranchCIRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.EconomicBranchCIResponse;
import bg.energo.phoenix.repository.nomenclature.customer.EconomicBranchCIRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EconomicBranchCIService implements NomenclatureBaseService {

    private final EconomicBranchCIRepository economicBranchCIRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.ECONOMIC_BRANCH_CI;
    }

    /**
     * Filters {@link EconomicBranchCI} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link EconomicBranchCI}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_CI, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering EconomicBranchCI list with statuses: {}", request);
        return economicBranchCIRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link EconomicBranchCI} item in the EconomicBranchCI list to a specified position.
     * The method retrieves the {@link EconomicBranchCI} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link EconomicBranchCI} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link EconomicBranchCI} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_CI, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in EconomicBranchCIs to top", request.getId());

        EconomicBranchCI economicBranchCI = economicBranchCIRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-EconomicBranchCI not found"));

        Long start;
        Long end;
        List<EconomicBranchCI> economicBranchCIS;

        if (economicBranchCI.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = economicBranchCI.getOrderingId();
            economicBranchCIS = economicBranchCIRepository.findInOrderingIdRange(start, end, economicBranchCI.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (EconomicBranchCI e : economicBranchCIS) {
                e.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = economicBranchCI.getOrderingId();
            end = request.getOrderingId();
            economicBranchCIS = economicBranchCIRepository.findInOrderingIdRange(start, end, economicBranchCI.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (EconomicBranchCI e : economicBranchCIS) {
                e.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        economicBranchCI.setOrderingId(request.getOrderingId());
        economicBranchCIS.add(economicBranchCI);
        economicBranchCIRepository.saveAll(economicBranchCIS);
    }

    /**
     * Sorts all {@link EconomicBranchCI} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_CI, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the EconomicBranchCIs alphabetically");
        List<EconomicBranchCI> economicBranchCIS = economicBranchCIRepository.orderByName();
        long orderingId = 1;

        for (EconomicBranchCI e : economicBranchCIS) {
            e.setOrderingId(orderingId);
            orderingId++;
        }

        economicBranchCIRepository.saveAll(economicBranchCIS);
    }

    /**
     * Deletes {@link EconomicBranchCI} if the validations are passed.
     *
     * @param id ID of the {@link EconomicBranchCI}
     * @throws DomainEntityNotFoundException if {@link EconomicBranchCI} is not found.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchCI} is already deleted.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchCI} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = ECONOMIC_BRANCH_CI, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing EconomicBranchCI with ID: {}", id);
        EconomicBranchCI economicBranchCI = economicBranchCIRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-EconomicBranchCI not found"));

        if (economicBranchCI.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        if (economicBranchCIRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        economicBranchCI.setStatus(DELETED);
        economicBranchCIRepository.save(economicBranchCI);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return economicBranchCIRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return economicBranchCIRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link EconomicBranchCI} at the end with the highest ordering ID.
     * If the request asks to save {@link EconomicBranchCI} as a default and a default {@link EconomicBranchCI} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link EconomicBranchCI}
     * @return {@link EconomicBranchCIResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public EconomicBranchCIResponse add(EconomicBranchCIRequest request) {
        log.debug("Adding EconomicBranchCI: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (economicBranchCIRepository.countEconomicBranchCIByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-EconomicBranchCI with the same name already exists;");
            throw new OperationNotAllowedException("name-EconomicBranchCI with the same name already exists;");
        }

        Long lastSortOrder = economicBranchCIRepository.findLastOrderingId();
        EconomicBranchCI economicBranchCI = new EconomicBranchCI(request);
        economicBranchCI.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<EconomicBranchCI> currentDefaultEconomicBranchCIOptional = economicBranchCIRepository.findByDefaultSelectionTrue();
            if (currentDefaultEconomicBranchCIOptional.isPresent()) {
                EconomicBranchCI currentDefaultEconomicBranchCI = currentDefaultEconomicBranchCIOptional.get();
                currentDefaultEconomicBranchCI.setDefaultSelection(false);
                economicBranchCIRepository.save(currentDefaultEconomicBranchCI);
            }
        }
        EconomicBranchCI economicBranchCIEntity = economicBranchCIRepository.save(economicBranchCI);
        return new EconomicBranchCIResponse(economicBranchCIEntity);
    }

    /**
     * Retrieves detailed information about {@link EconomicBranchCI} by ID
     *
     * @param id ID of {@link EconomicBranchCI}
     * @return {@link EconomicBranchCIResponse}
     * @throws DomainEntityNotFoundException if no {@link EconomicBranchCI} was found with the provided ID.
     */
    public EconomicBranchCIResponse view(Long id) {
        log.debug("Fetching EconomicBranchCI with ID: {}", id);
        EconomicBranchCI economicBranchCI = economicBranchCIRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("EconomicBranchCI not found"));
        return new EconomicBranchCIResponse(economicBranchCI);
    }

    /**
     * Edit the requested {@link EconomicBranchCI}.
     * If the request asks to save {@link EconomicBranchCI} as a default and a default {@link EconomicBranchCI} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link EconomicBranchCI}
     * @param request {@link EconomicBranchCIRequest}
     * @return {@link EconomicBranchCIResponse}
     * @throws DomainEntityNotFoundException if {@link EconomicBranchCI} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link EconomicBranchCI} is deleted.
     */
    @Transactional
    public EconomicBranchCIResponse edit(Long id, EconomicBranchCIRequest request) {
        log.debug("Editing EconomicBranchCI: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        EconomicBranchCI economicBranchCI = economicBranchCIRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-EconomicBranchCI not found"));

        if (economicBranchCIRepository.countEconomicBranchCIByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !economicBranchCI.getName().equals(request.getName().trim())) {
            log.error("name-EconomicBranchCI with the same name already exists;");
            throw new OperationNotAllowedException("name-EconomicBranchCI with the same name already exists;");
        }

        if (economicBranchCI.getStatus().equals(DELETED)) {
            log.error("status-Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !economicBranchCI.isDefaultSelection()) {
            Optional<EconomicBranchCI> currentDefaultEconomicBranchCIOptional = economicBranchCIRepository.findByDefaultSelectionTrue();
            if (currentDefaultEconomicBranchCIOptional.isPresent()) {
                EconomicBranchCI currentDefaultEconomicBranchCI = currentDefaultEconomicBranchCIOptional.get();
                currentDefaultEconomicBranchCI.setDefaultSelection(false);
                economicBranchCIRepository.save(currentDefaultEconomicBranchCI);
            }
        }
        economicBranchCI.setDefaultSelection(request.getDefaultSelection());

        economicBranchCI.setName(request.getName().trim());
        economicBranchCI.setStatus(request.getStatus());
        return new EconomicBranchCIResponse(economicBranchCIRepository.save(economicBranchCI));
    }

    /**
     * Filters {@link EconomicBranchCI} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link EconomicBranchCI}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<EconomicBranchCIResponse> Page&lt;EconomicBranchCIResponse&gt;} containing detailed information
     */
    public Page<EconomicBranchCIResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering EconomicBranchCIs list with statuses: {}", request.toString());
        Page<EconomicBranchCI> page = economicBranchCIRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(EconomicBranchCIResponse::new);
    }
}
