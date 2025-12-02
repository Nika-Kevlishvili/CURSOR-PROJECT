package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForCancellation;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.ReasonForCancellationRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.ReasonForCancellationResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.receivable.ReasonForCancellationRepository;
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
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReasonForCancellationService implements NomenclatureBaseService {
    private final ReasonForCancellationRepository reasonForCancellationRepository;

    @Transactional
    public ReasonForCancellationResponse create(ReasonForCancellationRequest request) {
        log.debug("Adding reason for cancellation : {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("can not add reason with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (reasonForCancellationRepository.existsReasonForDisconnectionWithNameAndStatus(request.getName().trim(), List.of(ACTIVE, INACTIVE))) {
            throw new ClientException("Reason for cancellation with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = reasonForCancellationRepository.lastOrderingId();
        ReasonForCancellation reasonForCancellation = new ReasonForCancellation(request);
        reasonForCancellation.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        checkDefaultSelectionAdd(request.getStatus(), reasonForCancellation, request.getDefaultSelection());
        reasonForCancellationRepository.save(reasonForCancellation);
        return new ReasonForCancellationResponse(reasonForCancellation);
    }

    @Transactional
    public ReasonForCancellationResponse edit(Long id, ReasonForCancellationRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ReasonForCancellation reasonForCancellation = reasonForCancellationRepository.findByIdAndStatuses(id, List.of(NomenclatureItemStatus.ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Cancellation not found"));

        if (reasonForCancellationRepository.existsReasonForDisconnectionWithNameAndStatus(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                && !reasonForCancellation.getName().equals(request.getName().trim())) {
            throw new ClientException("Reason for Cancellation with such name already exists!", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        checkDefaultSelectionEdit(request.getStatus(), reasonForCancellation, request.getDefaultSelection());

        reasonForCancellation.setName(request.getName().trim());
        reasonForCancellation.setStatus(request.getStatus());

        return new ReasonForCancellationResponse(reasonForCancellation);
    }

    public ReasonForCancellationResponse view(Long id) {
        ReasonForCancellation reasonForCancellation = reasonForCancellationRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Cancellation not found!"));
        return new ReasonForCancellationResponse(reasonForCancellation);
    }

    public Page<ReasonForCancellationResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        return reasonForCancellationRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        ).map(ReasonForCancellationResponse::new);
    }


    private void checkDefaultSelectionEdit(NomenclatureItemStatus status, ReasonForCancellation reasonForCancellation, boolean defaultSelection) {
        if (status.equals(INACTIVE)) {
            reasonForCancellation.setDefaultSelection(false);
        } else {
            if (defaultSelection) {
                if (!reasonForCancellation.isDefaultSelection()) {
                    Optional<ReasonForCancellation> currentDefault = reasonForCancellationRepository.findByDefaultSelectionTrue();
                    if (currentDefault.isPresent()) {
                        ReasonForCancellation currentReason = currentDefault.get();
                        currentReason.setDefaultSelection(false);
                        reasonForCancellationRepository.save(currentReason);
                    }
                }
            }
            reasonForCancellation.setDefaultSelection(defaultSelection);
        }
    }


    private void checkDefaultSelectionAdd(NomenclatureItemStatus status, ReasonForCancellation reasonForCancellation, boolean defaultSelection) {
        if (status.equals(INACTIVE)) {
            reasonForCancellation.setDefaultSelection(false);
        } else {
            if (defaultSelection) {
                Optional<ReasonForCancellation> currentDefault = reasonForCancellationRepository.findByDefaultSelectionTrue();
                if (currentDefault.isPresent()) {
                    ReasonForCancellation currentReason = currentDefault.get();
                    currentReason.setDefaultSelection(false);
                    reasonForCancellationRepository.save(currentReason);
                }
            }
            reasonForCancellation.setDefaultSelection(defaultSelection);
        }
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.REASON_FOR_CANCELLATION;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_CANCELLATION, permissions = NOMENCLATURE_VIEW)
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return reasonForCancellationRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize()));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_CANCELLATION, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        ReasonForCancellation reasonForCancellation = reasonForCancellationRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("Reason For Cancellation not found!", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ReasonForCancellation> reasonForDisconnections;

        if (reasonForCancellation.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = reasonForCancellation.getOrderingId();
            reasonForDisconnections = reasonForCancellationRepository.findInOrderingIdRange(
                    start,
                    end,
                    reasonForCancellation.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );
            long tempOrderingId = request.getOrderingId() + 1;
            for (ReasonForCancellation rc : reasonForDisconnections) {
                rc.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = reasonForCancellation.getOrderingId();
            end = request.getOrderingId();
            reasonForDisconnections = reasonForCancellationRepository.findInOrderingIdRange(
                    start,
                    end,
                    reasonForCancellation.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ReasonForCancellation rc : reasonForDisconnections) {
                rc.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        reasonForCancellation.setOrderingId(request.getOrderingId());
        reasonForCancellationRepository.save(reasonForCancellation);
        reasonForCancellationRepository.saveAll(reasonForDisconnections);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_CANCELLATION, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void sortAlphabetically() {
        List<ReasonForCancellation> reasonForCancellations = reasonForCancellationRepository.orderByName();
        long orderingId = 1;

        for (ReasonForCancellation reasonForCancellation : reasonForCancellations) {
            reasonForCancellation.setOrderingId(orderingId++);
        }

        reasonForCancellationRepository.saveAll(reasonForCancellations);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_CANCELLATION, permissions = NOMENCLATURE_EDIT)
            }
    )
    @Transactional
    public void delete(Long id) {
        ReasonForCancellation reasonForCancellation = reasonForCancellationRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Cancellation not found"));

        if (reasonForCancellation.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new ClientException("id-Item is already deleted.", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        boolean hasActiveConnections = reasonForCancellationRepository.hasActiveConnections(id);
        if (hasActiveConnections) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        reasonForCancellation.setDefaultSelection(false);
        reasonForCancellation.setStatus(DELETED);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return reasonForCancellationRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return List.of();
    }
}
