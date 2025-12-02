package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbg;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbgRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbgResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbgRepository;
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
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroundForObjectionWithdrawalToChangeOfACbgService implements NomenclatureBaseService {

    private final GroundForObjectionWithdrawalToChangeOfACbgRepository groundForObjectionWithdrawalToChangeOfACbgRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ground for objection withdrawal to change of a cbg with statuses: {}", request);
        Page<GroundForObjectionWithdrawalToChangeOfACbg> groundForObjectionWithdrawalToChangeOfACbgs = groundForObjectionWithdrawalToChangeOfACbgRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return groundForObjectionWithdrawalToChangeOfACbgs.map(this::nomenclatureResponseFromEntity);
    }

    public Page<GroundForObjectionWithdrawalToChangeOfACbgResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ground for objection withdrawal to change of a cbg list with request: {}", request);
        Page<GroundForObjectionWithdrawalToChangeOfACbg> groundForObjectionWithdrawalToChangeOfACbgs = groundForObjectionWithdrawalToChangeOfACbgRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return groundForObjectionWithdrawalToChangeOfACbgs.map(this::responseFromEntity);
    }


    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in ground for objection withdrawal to change of a cbg", request.getId());

        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Ground for objection withdrawal to change of a cbg not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<GroundForObjectionWithdrawalToChangeOfACbg> groundForObjectionWithdrawalToChangeOfACbgs;

        if (groundForObjectionWithdrawalToChangeOfACbg.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = groundForObjectionWithdrawalToChangeOfACbg.getOrderingId();
            groundForObjectionWithdrawalToChangeOfACbgs = groundForObjectionWithdrawalToChangeOfACbgRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            groundForObjectionWithdrawalToChangeOfACbg.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (GroundForObjectionWithdrawalToChangeOfACbg gfowtcoac : groundForObjectionWithdrawalToChangeOfACbgs) {
                gfowtcoac.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = groundForObjectionWithdrawalToChangeOfACbg.getOrderingId();
            end = request.getOrderingId();
            groundForObjectionWithdrawalToChangeOfACbgs = groundForObjectionWithdrawalToChangeOfACbgRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            groundForObjectionWithdrawalToChangeOfACbg.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (GroundForObjectionWithdrawalToChangeOfACbg gfowtcoa : groundForObjectionWithdrawalToChangeOfACbgs) {
                gfowtcoa.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        groundForObjectionWithdrawalToChangeOfACbg.setOrderingId(request.getOrderingId());
        groundForObjectionWithdrawalToChangeOfACbgs.add(groundForObjectionWithdrawalToChangeOfACbg);
        groundForObjectionWithdrawalToChangeOfACbgRepository.saveAll(groundForObjectionWithdrawalToChangeOfACbgs);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the ground for objection withdrawal to change of a cbg alphabetically");

        List<GroundForObjectionWithdrawalToChangeOfACbg> groundForObjectionWithdrawalToChangeOfACbgs = groundForObjectionWithdrawalToChangeOfACbgRepository.orderByName();
        long orderingId = 1;

        for (GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg : groundForObjectionWithdrawalToChangeOfACbgs) {
            groundForObjectionWithdrawalToChangeOfACbg.setOrderingId(orderingId);
            orderingId++;
        }

        groundForObjectionWithdrawalToChangeOfACbgRepository.saveAll(groundForObjectionWithdrawalToChangeOfACbgs);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing ground for objection withdrawal to change of a cbg with ID: {}", id);

        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Ground for objection withdrawal to change of a cbg not found, ID: " + id));

        if (groundForObjectionWithdrawalToChangeOfACbg.getStatus().equals(DELETED)) {
            log.error("Ground for objection withdrawal to change of a cbg {} is already deleted", id);
            throw new OperationNotAllowedException("id-Ground for objection withdrawal to change of a cbg " + id + " is already deleted");
        }
        if (groundForObjectionWithdrawalToChangeOfACbgRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        groundForObjectionWithdrawalToChangeOfACbg.setStatus(DELETED);
        groundForObjectionWithdrawalToChangeOfACbgRepository.save(groundForObjectionWithdrawalToChangeOfACbg);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return groundForObjectionWithdrawalToChangeOfACbgRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return groundForObjectionWithdrawalToChangeOfACbgRepository.findByIdIn(ids);
    }

    @Transactional
    public GroundForObjectionWithdrawalToChangeOfACbgResponse add(GroundForObjectionWithdrawalToChangeOfACbgRequest request) {
        log.debug("Adding Ground for objection withdrawal to change of a cbg: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (groundForObjectionWithdrawalToChangeOfACbgRepository.countGroundForObjectionWithdrawalToChangeOfACbgByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Ground for objection withdrawal to change of a cbg with such name already exists");
            throw new ClientException("ground-for objection withdrawal to change of a cbg with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = groundForObjectionWithdrawalToChangeOfACbgRepository.findLastOrderingId();
        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = entityFromRequest(request);
        groundForObjectionWithdrawalToChangeOfACbg.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, groundForObjectionWithdrawalToChangeOfACbg);

        groundForObjectionWithdrawalToChangeOfACbgRepository.save(groundForObjectionWithdrawalToChangeOfACbg);
        return responseFromEntity(groundForObjectionWithdrawalToChangeOfACbg);
    }

    public GroundForObjectionWithdrawalToChangeOfACbgResponse view(Long id) {
        log.debug("Fetching ground for objection withdrawal to change of a cbg with ID: {}", id);
        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Ground for objection withdrawal to change of a cbg ground not found, ID: " + id));

        return responseFromEntity(groundForObjectionWithdrawalToChangeOfACbg);
    }

    @Transactional
    public GroundForObjectionWithdrawalToChangeOfACbgResponse edit(Long id, GroundForObjectionWithdrawalToChangeOfACbgRequest request) {
        log.debug("Editing ground for objection withdrawal to change of a cbg: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Ground for objection withdrawal to change of a cbg not found, ID: " + id));

        if (groundForObjectionWithdrawalToChangeOfACbg.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (groundForObjectionWithdrawalToChangeOfACbgRepository.countGroundForObjectionWithdrawalToChangeOfACbgByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !groundForObjectionWithdrawalToChangeOfACbg.getName().equals(request.getName().trim())) {
            log.error("Ground for objection withdrawal to change of a cbg with such name already exists");
            throw new ClientException("ground-For objection withdrawal to change of a cbg with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, groundForObjectionWithdrawalToChangeOfACbg);

        groundForObjectionWithdrawalToChangeOfACbg.setName(request.getName().trim());
        groundForObjectionWithdrawalToChangeOfACbg.setStatus(request.getStatus());

        return responseFromEntity(groundForObjectionWithdrawalToChangeOfACbg);
    }


    private void assignDefaultSelectionWhenAdding(GroundForObjectionWithdrawalToChangeOfACbgRequest request, GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg) {
        if (request.getStatus().equals(INACTIVE)) {
            groundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<GroundForObjectionWithdrawalToChangeOfACbg> currentDefaultGroundForObjectionWithdrawalToChangeOfACbgOptional = groundForObjectionWithdrawalToChangeOfACbgRepository.findByDefaultSelectionTrue();
                if (currentDefaultGroundForObjectionWithdrawalToChangeOfACbgOptional.isPresent()) {
                    GroundForObjectionWithdrawalToChangeOfACbg currentDefaultGroundForObjectionWithdrawalToChangeOfACbg = currentDefaultGroundForObjectionWithdrawalToChangeOfACbgOptional.get();
                    currentDefaultGroundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(false);
                    groundForObjectionWithdrawalToChangeOfACbgRepository.save(currentDefaultGroundForObjectionWithdrawalToChangeOfACbg);
                }
            }
            groundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(GroundForObjectionWithdrawalToChangeOfACbgRequest request, GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg) {
        if (request.getStatus().equals(INACTIVE)) {
            groundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!groundForObjectionWithdrawalToChangeOfACbg.isDefaultSelection()) {
                    Optional<GroundForObjectionWithdrawalToChangeOfACbg> optionalGroundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository.findByDefaultSelectionTrue();
                    if (optionalGroundForObjectionWithdrawalToChangeOfACbg.isPresent()) {
                        GroundForObjectionWithdrawalToChangeOfACbg currentGroundForObjectionWithdrawalToChangeOfACbg = optionalGroundForObjectionWithdrawalToChangeOfACbg.get();
                        currentGroundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(false);
                        groundForObjectionWithdrawalToChangeOfACbgRepository.save(currentGroundForObjectionWithdrawalToChangeOfACbg);
                    }
                }
            }
            groundForObjectionWithdrawalToChangeOfACbg.setDefaultSelection(request.getDefaultSelection());
        }
    }


    public GroundForObjectionWithdrawalToChangeOfACbgResponse responseFromEntity(GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg) {
        return new GroundForObjectionWithdrawalToChangeOfACbgResponse(
                groundForObjectionWithdrawalToChangeOfACbg.getId(),
                groundForObjectionWithdrawalToChangeOfACbg.getName(),
                groundForObjectionWithdrawalToChangeOfACbg.getOrderingId(),
                groundForObjectionWithdrawalToChangeOfACbg.isDefaultSelection(),
                groundForObjectionWithdrawalToChangeOfACbg.getStatus()
        );
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg) {
        return new NomenclatureResponse(
                groundForObjectionWithdrawalToChangeOfACbg.getId(),
                groundForObjectionWithdrawalToChangeOfACbg.getName(),
                groundForObjectionWithdrawalToChangeOfACbg.getOrderingId(),
                groundForObjectionWithdrawalToChangeOfACbg.isDefaultSelection(),
                groundForObjectionWithdrawalToChangeOfACbg.getStatus()
        );
    }

    public GroundForObjectionWithdrawalToChangeOfACbg entityFromRequest(GroundForObjectionWithdrawalToChangeOfACbgRequest request) {
        return GroundForObjectionWithdrawalToChangeOfACbg
                .builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }
}
