package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForDisconnection;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.ReasonForDisconnectionRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.ReasonForDisconnectionResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.receivable.ReasonForDisconnectionRepository;
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
public class ReasonForDisconnectionService implements NomenclatureBaseService {

    private final ReasonForDisconnectionRepository reasonForDisconnectionRepository;

    @Transactional
    public ReasonForDisconnectionResponse add(ReasonForDisconnectionRequest request) {
        log.debug("Adding reason for disconnection : {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("can not add reason with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if(reasonForDisconnectionRepository.existsReasonForDisconnectionWithNameAndStatus(request.getName().trim(),List.of(ACTIVE,INACTIVE))) {
            throw new ClientException("Reason for disconnection with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = reasonForDisconnectionRepository.lastOrderingId();
        ReasonForDisconnection reasonForDisconnection = new ReasonForDisconnection(request);
        reasonForDisconnection.setOrderingId(lastOrderingId==null ? 1 : lastOrderingId+1);

        checkDefaultSelectionAdd(request.getStatus(),reasonForDisconnection,request.getDefaultSelection());
        reasonForDisconnectionRepository.save(reasonForDisconnection);
        return new ReasonForDisconnectionResponse(reasonForDisconnection);
    }

    @Transactional
    public ReasonForDisconnectionResponse edit(Long id,ReasonForDisconnectionRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ReasonForDisconnection reasonForDisconnection = reasonForDisconnectionRepository.findByIdAndStatuses(id, List.of(NomenclatureItemStatus.ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Disconnection not found"));

        if (reasonForDisconnectionRepository.existsReasonForDisconnectionWithNameAndStatus(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                && !reasonForDisconnection.getName().equals(request.getName().trim())) {
            throw new ClientException("Reason for disconnection with such name already exists!", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        checkDefaultSelectionEdit(request.getStatus(),reasonForDisconnection,request.getDefaultSelection());

        reasonForDisconnection.setName(request.getName().trim());
        reasonForDisconnection.setStatus(request.getStatus());

        return new ReasonForDisconnectionResponse(reasonForDisconnection);
    }


    public ReasonForDisconnectionResponse view(Long id) {
        ReasonForDisconnection reasonForDisconnection = reasonForDisconnectionRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Disconnection not found!"));
        return new ReasonForDisconnectionResponse(reasonForDisconnection);
    }

    public Page<ReasonForDisconnectionResponse> filter(NomenclatureItemsBaseFilterRequest request) {

        return reasonForDisconnectionRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        ).map(ReasonForDisconnectionResponse::new);
    }

    private void checkDefaultSelectionAdd(NomenclatureItemStatus status,ReasonForDisconnection reasonForDisconnection,boolean defaultSelection) {
        if(status.equals(INACTIVE)) {
            reasonForDisconnection.setDefaultSelection(false);
        } else {
            if(defaultSelection) {
                Optional<ReasonForDisconnection> currentDefault = reasonForDisconnectionRepository.findByDefaultSelectionTrue();
                if(currentDefault.isPresent()) {
                    ReasonForDisconnection currentReason = currentDefault.get();
                    currentReason.setDefaultSelection(false);
                    reasonForDisconnectionRepository.save(currentReason);
                }
            }
            reasonForDisconnection.setDefaultSelection(defaultSelection);
        }
    }

    private void checkDefaultSelectionEdit(NomenclatureItemStatus status,ReasonForDisconnection reasonForDisconnection,boolean defaultSelection) {
        if(status.equals(INACTIVE)) {
            reasonForDisconnection.setDefaultSelection(false);
        } else {
            if(defaultSelection) {
                if(!reasonForDisconnection.isDefaultSelection()) {
                    Optional<ReasonForDisconnection> currentDefault = reasonForDisconnectionRepository.findByDefaultSelectionTrue();
                    if (currentDefault.isPresent()) {
                        ReasonForDisconnection currentReason = currentDefault.get();
                        currentReason.setDefaultSelection(false);
                        reasonForDisconnectionRepository.save(currentReason);
                    }
                }
            }
            reasonForDisconnection.setDefaultSelection(defaultSelection);
        }
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.REASON_FOR_DISCONNECTION;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_DISCONNECTION, permissions = NOMENCLATURE_VIEW)
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return reasonForDisconnectionRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(),request.getSize()));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_DISCONNECTION, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        ReasonForDisconnection reasonForDisconnection = reasonForDisconnectionRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("Reason For Disconnection not found!", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<ReasonForDisconnection> reasonForDisconnections;

        if (reasonForDisconnection.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = reasonForDisconnection.getOrderingId();
            reasonForDisconnections = reasonForDisconnectionRepository.findInOrderingIdRange(
                    start,
                    end,
                    reasonForDisconnection.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );
            long tempOrderingId = request.getOrderingId() + 1;
            for (ReasonForDisconnection br : reasonForDisconnections) {
                br.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = reasonForDisconnection.getOrderingId();
            end = request.getOrderingId();
            reasonForDisconnections = reasonForDisconnectionRepository.findInOrderingIdRange(
                    start,
                    end,
                    reasonForDisconnection.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ReasonForDisconnection br : reasonForDisconnections) {
                br.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        reasonForDisconnection.setOrderingId(request.getOrderingId());
        reasonForDisconnectionRepository.save(reasonForDisconnection);
        reasonForDisconnectionRepository.saveAll(reasonForDisconnections);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_DISCONNECTION, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void sortAlphabetically() {

        List<ReasonForDisconnection> reasonForDisconnections = reasonForDisconnectionRepository.orderByName();
        long orderingId = 1;

        for(ReasonForDisconnection reasonForDisconnection : reasonForDisconnections) {
            reasonForDisconnection.setOrderingId(orderingId++);
        }

        reasonForDisconnectionRepository.saveAll(reasonForDisconnections);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.REASON_FOR_DISCONNECTION, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void delete(Long id) {

        ReasonForDisconnection reasonForDisconnection = reasonForDisconnectionRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reason For Disconnection not found"));

        if (reasonForDisconnection.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new ClientException("id-Item is already deleted.", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if(reasonForDisconnectionRepository.isConnectedToObj(id)) {
            log.error("Can't delete the nomenclature because it is connected to Request for Disconnection of Power Supply");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to Request for Disconnection of Power Supply");
        }
        reasonForDisconnection.setDefaultSelection(false);
        reasonForDisconnection.setStatus(DELETED);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return reasonForDisconnectionRepository.existsByIdAndStatusIn(id,statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return reasonForDisconnectionRepository.findByIdsIn(ids);
    }
}
