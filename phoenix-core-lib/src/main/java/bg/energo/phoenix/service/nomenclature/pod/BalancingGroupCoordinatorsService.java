package bg.energo.phoenix.service.nomenclature.pod;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.pod.BalancingGroupCoordinators;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.BalancingGroupCoordinatorsRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.BalancingGroupCoordinatorsResponse;
import bg.energo.phoenix.repository.nomenclature.pod.BalancingGroupCoordinatorsRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.COORDINATOR_BALANCING_GROUPS;
import static bg.energo.phoenix.permissions.PermissionContextEnum.POD;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalancingGroupCoordinatorsService implements NomenclatureBaseService {

    private final BalancingGroupCoordinatorsRepository balancingGroupCoordinatorsRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.BALANCING_GROUP_COORDINATORS;
    }

    /**
     * Filters {@link BalancingGroupCoordinators} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link BalancingGroupCoordinators}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COORDINATOR_BALANCING_GROUPS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = POD, permissions = {
                            POD_VIEW_BASIC,
                            POD_VIEW_DELETED
                            }),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering balancingGroupCoordinators list with statuses: {}", request);
        return balancingGroupCoordinatorsRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link BalancingGroupCoordinatorsService} item in the Bank list to a specified position.
     * The method retrieves the {@link BalancingGroupCoordinatorsService} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link BalancingGroupCoordinatorsService} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link BalancingGroupCoordinatorsService} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COORDINATOR_BALANCING_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in banks to top", request.getId());

        BalancingGroupCoordinators balancingGroupCoordinators = balancingGroupCoordinatorsRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Bank not found"));

        Long start;
        Long end;
        List<BalancingGroupCoordinators> balancingGroupCoordinatorsList;

        if (balancingGroupCoordinators.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = balancingGroupCoordinators.getOrderingId();
            balancingGroupCoordinatorsList = balancingGroupCoordinatorsRepository.findInOrderingIdRange(start, end, balancingGroupCoordinators.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (BalancingGroupCoordinators b : balancingGroupCoordinatorsList) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = balancingGroupCoordinators.getOrderingId();
            end = request.getOrderingId();
            balancingGroupCoordinatorsList = balancingGroupCoordinatorsRepository.findInOrderingIdRange(start, end, balancingGroupCoordinators.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (BalancingGroupCoordinators b : balancingGroupCoordinatorsList) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        balancingGroupCoordinators.setOrderingId(request.getOrderingId());
        balancingGroupCoordinatorsList.add(balancingGroupCoordinators);
        balancingGroupCoordinatorsRepository.saveAll(balancingGroupCoordinatorsList);
    }

    /**
     * Sorts all {@link BalancingGroupCoordinators} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COORDINATOR_BALANCING_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the balancing group coordinators alphabetically");
        List<BalancingGroupCoordinators> balancingGroupCoordinators = balancingGroupCoordinatorsRepository.orderByName();
        long orderingId = 1;

        for (BalancingGroupCoordinators b : balancingGroupCoordinators) {
            b.setOrderingId(orderingId);
            orderingId++;
        }

        balancingGroupCoordinatorsRepository.saveAll(balancingGroupCoordinators);
    }

    /**
     * Deletes {@link BalancingGroupCoordinators} if the validations are passed.
     *
     * @param id ID of the {@link BalancingGroupCoordinators}
     * @throws DomainEntityNotFoundException if {@link BalancingGroupCoordinators} is not found.
     * @throws OperationNotAllowedException  if the {@link BalancingGroupCoordinators} is already deleted.
     * @throws OperationNotAllowedException  if the {@link BalancingGroupCoordinators} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = COORDINATOR_BALANCING_GROUPS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing bank with ID: {}", id);
        BalancingGroupCoordinators balancingGroupCoordinators = balancingGroupCoordinatorsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Balancing group coordinators not found"));

        if (balancingGroupCoordinators.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (balancingGroupCoordinatorsRepository.getActiveConnectionsCount(id) > 0) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        balancingGroupCoordinators.setStatus(DELETED);
        balancingGroupCoordinatorsRepository.save(balancingGroupCoordinators);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return balancingGroupCoordinatorsRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return balancingGroupCoordinatorsRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link BalancingGroupCoordinators} at the end with the highest ordering ID.
     * If the request asks to save {@link BalancingGroupCoordinators} as a default and a default {@link BalancingGroupCoordinators} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link BalancingGroupCoordinatorsRequest}
     * @return {@link BalancingGroupCoordinatorsResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public BalancingGroupCoordinatorsResponse add(BalancingGroupCoordinatorsRequest request) {
        log.debug("Adding Balancing Group coordinators: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (balancingGroupCoordinatorsRepository.countBankByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Balancing Group Coordinator  with the same name already exists;");
            throw new OperationNotAllowedException("name-Balancing Group Coordinator with the same name already exists;");
        }

        Long lastSortOrder = balancingGroupCoordinatorsRepository.findLastOrderingId();
        BalancingGroupCoordinators balancingGroupCoordinators = new BalancingGroupCoordinators(request);
        balancingGroupCoordinators.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelectionWhenAdding(request, balancingGroupCoordinators);
        BalancingGroupCoordinators balancingGroupCoordinatorsEntity = balancingGroupCoordinatorsRepository.save(balancingGroupCoordinators);
        return new BalancingGroupCoordinatorsResponse(balancingGroupCoordinatorsEntity);
    }

    private void assignDefaultSelectionWhenAdding(BalancingGroupCoordinatorsRequest request, BalancingGroupCoordinators balancingGroupCoordinators) {
        if (request.getStatus().equals(INACTIVE)) {
            balancingGroupCoordinators.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<BalancingGroupCoordinators> currentbgcOptional =
                        balancingGroupCoordinatorsRepository.findByDefaultSelectionTrue();
                if (currentbgcOptional.isPresent()) {
                    BalancingGroupCoordinators currentDefaultGridOperator = currentbgcOptional.get();
                    currentDefaultGridOperator.setDefaultSelection(false);
                    balancingGroupCoordinatorsRepository.save(currentDefaultGridOperator);
                }
            }
            balancingGroupCoordinators.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Retrieves detailed information about {@link BalancingGroupCoordinators} by ID
     *
     * @param id ID of {@link BalancingGroupCoordinators}
     * @return {@link BalancingGroupCoordinatorsResponse}
     * @throws DomainEntityNotFoundException if no {@link BalancingGroupCoordinators} was found with the provided ID.
     */
    public BalancingGroupCoordinatorsResponse view(Long id) {
        log.debug("Fetching balancingGroupCoordinators with ID: {}", id);
        BalancingGroupCoordinators balancingGroupCoordinators = balancingGroupCoordinatorsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("balancingGroupCoordinators not found"));
        return new BalancingGroupCoordinatorsResponse(balancingGroupCoordinators);
    }

    /**
     * Edit the requested {@link BalancingGroupCoordinators}.
     * If the request asks to save {@link BalancingGroupCoordinators} as a default and a default {@link BalancingGroupCoordinators} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link BalancingGroupCoordinators}
     * @param request {@link BalancingGroupCoordinatorsRequest}
     * @return {@link BalancingGroupCoordinatorsResponse}
     * @throws DomainEntityNotFoundException if {@link BalancingGroupCoordinators} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link BalancingGroupCoordinators} is deleted.
     */
    @Transactional
    public BalancingGroupCoordinatorsResponse edit(Long id, BalancingGroupCoordinatorsRequest request) {
        log.debug("Editing BalancingGroupCoordinators: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        BalancingGroupCoordinators balancingGroupCoordinators = balancingGroupCoordinatorsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-BalancingGroupCoordinators not found"));

        if (balancingGroupCoordinatorsRepository.countBankByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !balancingGroupCoordinators.getName().equals(request.getName().trim())) {
            log.error("name-BalancingGroupCoordinators with the same name already exists;");
            throw new OperationNotAllowedException("name-BalancingGroupCoordinators with the same name already exists;");
        }

        if (balancingGroupCoordinators.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item");
        }

        if (request.getDefaultSelection() && !balancingGroupCoordinators.isDefaultSelection()) {
            Optional<BalancingGroupCoordinators> currentDefaultBankOptional = balancingGroupCoordinatorsRepository.findByDefaultSelectionTrue();
            if (currentDefaultBankOptional.isPresent()) {
                BalancingGroupCoordinators currentDefaultBank = currentDefaultBankOptional.get();
                currentDefaultBank.setDefaultSelection(false);
                balancingGroupCoordinatorsRepository.save(currentDefaultBank);
            }
        }
        assignDefaultSelectionWhenEditing(request, balancingGroupCoordinators);

        balancingGroupCoordinators.setName(request.getName().trim());
        balancingGroupCoordinators.setFullName(request.getFullName().trim());
        balancingGroupCoordinators.setStatus(request.getStatus());
        return new BalancingGroupCoordinatorsResponse(balancingGroupCoordinatorsRepository.save(balancingGroupCoordinators));
    }

    private void assignDefaultSelectionWhenEditing(BalancingGroupCoordinatorsRequest request, BalancingGroupCoordinators balancingGroupCoordinators) {
        if (request.getStatus().equals(INACTIVE)) {
            balancingGroupCoordinators.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!balancingGroupCoordinators.isDefaultSelection()) {
                    Optional<BalancingGroupCoordinators> currentDefaultbgcOptional =
                            balancingGroupCoordinatorsRepository.findByDefaultSelectionTrue();
                    if (currentDefaultbgcOptional.isPresent()) {
                        BalancingGroupCoordinators currentDefaultBgc = currentDefaultbgcOptional.get();
                        currentDefaultBgc.setDefaultSelection(false);
                        balancingGroupCoordinatorsRepository.save(currentDefaultBgc);
                    }
                }
            }
            balancingGroupCoordinators.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Filters {@link BalancingGroupCoordinators} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Bank}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<BalancingGroupCoordinatorsResponse> Page&lt;BalancingGroupCoordinatorsResponse&gt;} containing detailed information
     */
    public Page<BalancingGroupCoordinatorsResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering banks list with: {}", request.toString());
        Page<BalancingGroupCoordinators> page = balancingGroupCoordinatorsRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        return page.map(BalancingGroupCoordinatorsResponse::new);
    }
}
