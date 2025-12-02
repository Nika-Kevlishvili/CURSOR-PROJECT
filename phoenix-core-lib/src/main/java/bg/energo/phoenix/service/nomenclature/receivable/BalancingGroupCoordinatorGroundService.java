package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BalancingGroupCoordinatorGround;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.BalancingGroupCoordinatorGroundRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.BalancingGroupCoordinatorGroundResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.receivable.BalancingGroupCoordinatorGroundRepository;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.BALANCING_GROUP_COORDINATOR_GROUND;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalancingGroupCoordinatorGroundService implements NomenclatureBaseService {

    private final BalancingGroupCoordinatorGroundRepository balancingGroupCoordinatorGroundRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return BALANCING_GROUP_COORDINATOR_GROUND;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.BALANCING_GROUP_COORDINATOR_GROUND,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering balancing group coordinator grounds with statuses: {}", request);
        Page<BalancingGroupCoordinatorGround> balancingGroupCoordinatorGrounds = balancingGroupCoordinatorGroundRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return balancingGroupCoordinatorGrounds.map(this::nomenclatureResponseFromEntity);
    }

    public Page<BalancingGroupCoordinatorGroundResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering balancing group coordinator grounds list with request: {}", request);
        Page<BalancingGroupCoordinatorGround> balancingGroupCoordinatorGrounds = balancingGroupCoordinatorGroundRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return balancingGroupCoordinatorGrounds.map(this::responseFromEntity);
    }


    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.BALANCING_GROUP_COORDINATOR_GROUND,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in balancing group coordinator grounds", request.getId());

        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Balancing group coordinator grounds not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<BalancingGroupCoordinatorGround> balancingGroupCoordinatorGrounds;

        if (balancingGroupCoordinatorGround.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = balancingGroupCoordinatorGround.getOrderingId();
            balancingGroupCoordinatorGrounds = balancingGroupCoordinatorGroundRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            balancingGroupCoordinatorGround.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (BalancingGroupCoordinatorGround bgcg : balancingGroupCoordinatorGrounds) {
                bgcg.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = balancingGroupCoordinatorGround.getOrderingId();
            end = request.getOrderingId();
            balancingGroupCoordinatorGrounds = balancingGroupCoordinatorGroundRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            balancingGroupCoordinatorGround.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (BalancingGroupCoordinatorGround bgcg : balancingGroupCoordinatorGrounds) {
                bgcg.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        balancingGroupCoordinatorGround.setOrderingId(request.getOrderingId());
        balancingGroupCoordinatorGrounds.add(balancingGroupCoordinatorGround);
        balancingGroupCoordinatorGroundRepository.saveAll(balancingGroupCoordinatorGrounds);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.BALANCING_GROUP_COORDINATOR_GROUND,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the balancing group coordinator grounds alphabetically");

        List<BalancingGroupCoordinatorGround> balancingGroupCoordinatorGrounds = balancingGroupCoordinatorGroundRepository.orderByName();
        long orderingId = 1;

        for (BalancingGroupCoordinatorGround balancingGroupCoordinatorGround : balancingGroupCoordinatorGrounds) {
            balancingGroupCoordinatorGround.setOrderingId(orderingId);
            orderingId++;
        }

        balancingGroupCoordinatorGroundRepository.saveAll(balancingGroupCoordinatorGrounds);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.BALANCING_GROUP_COORDINATOR_GROUND,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing balancing group coordinator ground with ID: {}", id);

        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Balancing group coordinator grounds not found, ID: " + id));

        if (balancingGroupCoordinatorGround.getStatus().equals(DELETED)) {
            log.error("Balancing group coordinator ground {} is already deleted", id);
            throw new OperationNotAllowedException("id-balancing group coordinator ground " + id + " is already deleted");
        }
        if (balancingGroupCoordinatorGroundRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }
        balancingGroupCoordinatorGround.setStatus(DELETED);
        balancingGroupCoordinatorGroundRepository.save(balancingGroupCoordinatorGround);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return balancingGroupCoordinatorGroundRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return balancingGroupCoordinatorGroundRepository.findByIdIn(ids);
    }

    @Transactional
    public BalancingGroupCoordinatorGroundResponse add(BalancingGroupCoordinatorGroundRequest request) {
        log.debug("Adding balancing group coordinator ground: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (balancingGroupCoordinatorGroundRepository.countBalancingGroupCoordinatorGroundByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Balancing group coordinator ground with such name already exists");
            throw new ClientException("balancing-group coordinator ground with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = balancingGroupCoordinatorGroundRepository.findLastOrderingId();
        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = entityFromRequest(request);
        balancingGroupCoordinatorGround.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, balancingGroupCoordinatorGround);

        balancingGroupCoordinatorGroundRepository.save(balancingGroupCoordinatorGround);
        return responseFromEntity(balancingGroupCoordinatorGround);
    }

    public BalancingGroupCoordinatorGroundResponse view(Long id) {
        log.debug("Fetching balancing group coordinator ground with ID: {}", id);
        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Balancing group coordinator ground not found, ID: " + id));

        return responseFromEntity(balancingGroupCoordinatorGround);
    }

    @Transactional
    public BalancingGroupCoordinatorGroundResponse edit(Long id, BalancingGroupCoordinatorGroundRequest request) {
        log.debug("Editing balancing group coordinator ground: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Balancing group coordinator grounds not found, ID: " + id));

        if (balancingGroupCoordinatorGround.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (balancingGroupCoordinatorGroundRepository.countBalancingGroupCoordinatorGroundByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
            && !balancingGroupCoordinatorGround.getName().equals(request.getName().trim())) {
            log.error("Balancing group coordinator grounds with such name already exists");
            throw new ClientException("balancing-group coordinator grounds with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, balancingGroupCoordinatorGround);

        balancingGroupCoordinatorGround.setName(request.getName().trim());
        balancingGroupCoordinatorGround.setStatus(request.getStatus());

        return responseFromEntity(balancingGroupCoordinatorGround);
    }


    private void assignDefaultSelectionWhenAdding(BalancingGroupCoordinatorGroundRequest request, BalancingGroupCoordinatorGround balancingGroupCoordinatorGround) {
        if (request.getStatus().equals(INACTIVE)) {
            balancingGroupCoordinatorGround.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<BalancingGroupCoordinatorGround> currentDefaultBalancingGroupCoordinatorGroundOptional = balancingGroupCoordinatorGroundRepository.findByDefaultSelectionTrue();
                if (currentDefaultBalancingGroupCoordinatorGroundOptional.isPresent()) {
                    BalancingGroupCoordinatorGround currentDefaultBalancingGroupCoordinatorGround = currentDefaultBalancingGroupCoordinatorGroundOptional.get();
                    currentDefaultBalancingGroupCoordinatorGround.setDefaultSelection(false);
                    balancingGroupCoordinatorGroundRepository.save(currentDefaultBalancingGroupCoordinatorGround);
                }
            }
            balancingGroupCoordinatorGround.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(BalancingGroupCoordinatorGroundRequest request, BalancingGroupCoordinatorGround balancingGroupCoordinatorGround) {
        if (request.getStatus().equals(INACTIVE)) {
            balancingGroupCoordinatorGround.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!balancingGroupCoordinatorGround.isDefaultSelection()) {
                    Optional<BalancingGroupCoordinatorGround> optionalBalancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository.findByDefaultSelectionTrue();
                    if (optionalBalancingGroupCoordinatorGround.isPresent()) {
                        BalancingGroupCoordinatorGround currentBalancingGroupCoordinatorGround = optionalBalancingGroupCoordinatorGround.get();
                        currentBalancingGroupCoordinatorGround.setDefaultSelection(false);
                        balancingGroupCoordinatorGroundRepository.save(currentBalancingGroupCoordinatorGround);
                    }
                }
            }
            balancingGroupCoordinatorGround.setDefaultSelection(request.getDefaultSelection());
        }
    }


    public BalancingGroupCoordinatorGroundResponse responseFromEntity(BalancingGroupCoordinatorGround balancingGroupCoordinatorGround) {
        return new BalancingGroupCoordinatorGroundResponse(
                balancingGroupCoordinatorGround.getId(),
                balancingGroupCoordinatorGround.getName(),
                balancingGroupCoordinatorGround.getOrderingId(),
                balancingGroupCoordinatorGround.isDefaultSelection(),
                balancingGroupCoordinatorGround.getStatus()
        );
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(BalancingGroupCoordinatorGround balancingGroupCoordinatorGround) {
        return new NomenclatureResponse(
                balancingGroupCoordinatorGround.getId(),
                balancingGroupCoordinatorGround.getName(),
                balancingGroupCoordinatorGround.getOrderingId(),
                balancingGroupCoordinatorGround.isDefaultSelection(),
                balancingGroupCoordinatorGround.getStatus()
        );
    }

    public BalancingGroupCoordinatorGround entityFromRequest(BalancingGroupCoordinatorGroundRequest request) {
        return BalancingGroupCoordinatorGround
                .builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }
}
