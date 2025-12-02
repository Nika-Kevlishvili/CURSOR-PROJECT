package bg.energo.phoenix.service.nomenclature.crm;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.crm.SmsSenderNumberRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.crm.SmsSendingNumberResponse;
import bg.energo.phoenix.repository.nomenclature.crm.SmsSendingNumberRepository;
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
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SMS_SENDING_NUMBERS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSendingNumberService implements NomenclatureBaseService {

    private final SmsSendingNumberRepository smsSendingNumberRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SMS_SENDING_NUMBERS;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SMS_SENDING_NUMBERS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sms sending numbers with statuses: {}", request);
        Page<SmsSendingNumber> smsSendingNumbers = smsSendingNumberRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return smsSendingNumbers.map(this::nomenclatureResponseFromEntity);
    }

    public Page<SmsSendingNumberResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering sms sending numbers list with request: {}", request);
        Page<SmsSendingNumber> smsSendingNumbers = smsSendingNumberRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return smsSendingNumbers.map(this::responseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SMS_SENDING_NUMBERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in sms sending numbers", request.getId());

        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sms sending number not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<SmsSendingNumber> smsSendingNumberList;

        if (smsSendingNumber.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = smsSendingNumber.getOrderingId();
            smsSendingNumberList = smsSendingNumberRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            smsSendingNumber.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (SmsSendingNumber ra : smsSendingNumberList) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = smsSendingNumber.getOrderingId();
            end = request.getOrderingId();
            smsSendingNumberList = smsSendingNumberRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            smsSendingNumber.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (SmsSendingNumber ra : smsSendingNumberList) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        smsSendingNumber.setOrderingId(request.getOrderingId());
        smsSendingNumberList.add(smsSendingNumber);
        smsSendingNumberRepository.saveAll(smsSendingNumberList);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SMS_SENDING_NUMBERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the sms sending numbers alphabetically");

        List<SmsSendingNumber> smsSendingNumberList = smsSendingNumberRepository.orderByName();
        long orderingId = 1;

        for (SmsSendingNumber smsSendingNumber : smsSendingNumberList) {
            smsSendingNumber.setOrderingId(orderingId);
            orderingId++;
        }

        smsSendingNumberRepository.saveAll(smsSendingNumberList);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SMS_SENDING_NUMBERS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing sms sending number with ID: {}", id);

        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sms sending number not found, ID: " + id));

        boolean hasActiveConnections = smsSendingNumberRepository.hasActiveConnections(id);
        if (hasActiveConnections) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (smsSendingNumber.getStatus().equals(DELETED)) {
            log.error("Sms sending number {} is already deleted", id);
            throw new OperationNotAllowedException("id-Sms sending number " + id + " is already deleted");
        }

        if (smsSendingNumber.isHardCoded()) {
            log.error("id-You can't delete hard-coded nomenclature item;");
            throw new OperationNotAllowedException("id-You can't delete hard-coded nomenclature item;");
        }

        smsSendingNumber.setStatus(DELETED);
        smsSendingNumberRepository.save(smsSendingNumber);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return smsSendingNumberRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return smsSendingNumberRepository.findByIdIn(ids);
    }

    @Transactional
    public SmsSendingNumberResponse add(SmsSenderNumberRequest request) {
        log.debug("Adding sms sending number: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (smsSendingNumberRepository.countSmsSendingNumbersByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Sms sending number with such name already exists");
            throw new ClientException("Sms sending number with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = smsSendingNumberRepository.findLastOrderingId();
        SmsSendingNumber smsSendingNumber = entityFromRequest(request);
        smsSendingNumber.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, smsSendingNumber);

        smsSendingNumberRepository.save(smsSendingNumber);
        return responseFromEntity(smsSendingNumber);
    }

    public SmsSendingNumberResponse view(Long id) {
        log.debug("Fetching sms sending number with ID: {}", id);
        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sms sending number not found, ID: " + id));

        return responseFromEntity(smsSendingNumber);
    }

    @Transactional
    public SmsSendingNumberResponse edit(Long id, SmsSenderNumberRequest request) {
        log.debug("Editing sms sending number: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Sms sending number not found, ID: " + id));

        if (smsSendingNumber.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (smsSendingNumberRepository.countSmsSendingNumbersByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !smsSendingNumber.getName().equals(request.getName().trim())) {
            log.error("Sms sending number with such name already exists");
            throw new ClientException("Sms sending number with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (!Objects.equals(smsSendingNumber.getSmsNumber(), request.getSmsNumber())) {
            log.error("Can not edit SMS sending number");
            throw new OperationNotAllowedException("smsNumber-Cannot edit smsNumber");
        }

        if (smsSendingNumber.isHardCoded()) {
            log.error("id-You can't edit hard-coded nomenclature item;");
            throw new OperationNotAllowedException("id-You can't edit hard-coded nomenclature item;");
        }

        assignDefaultSelectionWhenEditing(request, smsSendingNumber);

        smsSendingNumber.setName(request.getName().trim());
        smsSendingNumber.setStatus(request.getStatus());

        return responseFromEntity(smsSendingNumber);
    }

    private void assignDefaultSelectionWhenAdding(SmsSenderNumberRequest request, SmsSendingNumber smsSendingNumber) {
        if (request.getStatus().equals(INACTIVE)) {
            smsSendingNumber.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<SmsSendingNumber> currentDefaultSmsSendingNumberOptional = smsSendingNumberRepository.findByDefaultSelectionTrue();
                if (currentDefaultSmsSendingNumberOptional.isPresent()) {
                    SmsSendingNumber currentDefaultSmsSendingNumber = currentDefaultSmsSendingNumberOptional.get();
                    currentDefaultSmsSendingNumber.setDefaultSelection(false);
                    smsSendingNumberRepository.save(currentDefaultSmsSendingNumber);
                }
            }
            smsSendingNumber.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(SmsSenderNumberRequest request, SmsSendingNumber smsSendingNumber) {
        if (request.getStatus().equals(INACTIVE)) {
            smsSendingNumber.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!smsSendingNumber.isDefaultSelection()) {
                    Optional<SmsSendingNumber> optionalSmsSendingNumber = smsSendingNumberRepository.findByDefaultSelectionTrue();
                    if (optionalSmsSendingNumber.isPresent()) {
                        SmsSendingNumber currentSmsSendingNumber = optionalSmsSendingNumber.get();
                        currentSmsSendingNumber.setDefaultSelection(false);
                        smsSendingNumberRepository.save(currentSmsSendingNumber);
                    }
                }
            }
            smsSendingNumber.setDefaultSelection(request.getDefaultSelection());
        }
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(SmsSendingNumber smsSendingNumber) {
        return new NomenclatureResponse(
                smsSendingNumber.getId(),
                smsSendingNumber.getName(),
                smsSendingNumber.getOrderingId(),
                smsSendingNumber.isDefaultSelection(),
                smsSendingNumber.getStatus()
        );
    }

    public SmsSendingNumberResponse responseFromEntity(SmsSendingNumber smsSendingNumber) {
        return new SmsSendingNumberResponse(
                smsSendingNumber.getId(),
                smsSendingNumber.getName(),
                smsSendingNumber.getOrderingId(),
                smsSendingNumber.isDefaultSelection(),
                smsSendingNumber.getStatus(),
                smsSendingNumber.getSmsNumber(),
                smsSendingNumber.isHardCoded()
        );
    }

    public SmsSendingNumber entityFromRequest(SmsSenderNumberRequest request) {
        return SmsSendingNumber
                .builder()
                .name(request.getName().trim())
                .smsNumber(request.getSmsNumber())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus())
                .isHardCoded(false)
                .build();
    }

}
