package bg.energo.phoenix.service.nomenclature.crm;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.crm.EmailMailboxesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.crm.EmailMailboxesResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMailboxesService implements NomenclatureBaseService {

    private final EmailMailboxesRepository emailMailboxesRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.EMAIL_MAILBOXES;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.EMAIL_MAILBOXES, permissions = NOMENCLATURE_VIEW)
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return emailMailboxesRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize()));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.EMAIL_MAILBOXES, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        EmailMailboxes emailMailboxes = emailMailboxesRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("Reason For Cancellation not found!", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<EmailMailboxes> emailMailboxesList;

        if (emailMailboxes.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = emailMailboxes.getOrderingId();
            emailMailboxesList = emailMailboxesRepository.findInOrderingIdRange(
                    start,
                    end,
                    emailMailboxes.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );
            long tempOrderingId = request.getOrderingId() + 1;
            for (EmailMailboxes em : emailMailboxesList) {
                em.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = emailMailboxes.getOrderingId();
            end = request.getOrderingId();
            emailMailboxesList = emailMailboxesRepository.findInOrderingIdRange(
                    start,
                    end,
                    emailMailboxes.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (EmailMailboxes em : emailMailboxesList) {
                em.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        emailMailboxes.setOrderingId(request.getOrderingId());
        emailMailboxesRepository.save(emailMailboxes);
        emailMailboxesRepository.saveAll(emailMailboxesList);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.EMAIL_MAILBOXES, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void sortAlphabetically() {
        List<EmailMailboxes> emailMailboxesList = emailMailboxesRepository.orderByName();
        long orderingId = 1;

        for (EmailMailboxes emailMailboxes : emailMailboxesList) {
            emailMailboxes.setOrderingId(orderingId++);
        }

        emailMailboxesRepository.saveAll(emailMailboxesList);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.EMAIL_MAILBOXES, permissions = NOMENCLATURE_EDIT)
            }
    )
    public void delete(Long id) {
        EmailMailboxes emailMailboxes = emailMailboxesRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mailboxes not found"));

        if (emailMailboxes.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new ClientException("id-Item is already deleted.", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        boolean hasActiveConnections = emailMailboxesRepository.hasActiveConnections(id);
        if (hasActiveConnections) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (emailMailboxes.getIsHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("Hardcoded nomenclature can't be deleted.;");
        }

        emailMailboxes.setDefaultSelection(false);
        emailMailboxes.setEmailForSendingInvoices(false);
        emailMailboxes.setEmailForGridOperator(false);
        emailMailboxes.setCommunicationForContract(false);
        emailMailboxes.setStatus(DELETED);

        emailMailboxesRepository.save(emailMailboxes);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return emailMailboxesRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return emailMailboxesRepository.findByIdsIn(ids);
    }

    @Transactional
    public EmailMailboxesResponse create(EmailMailboxesRequest request) {
        log.debug("Adding email mailboxes : {}", request);
        List<String> errorMessages = new ArrayList<>();

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("can not add E-Mail Mailboxes with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (emailMailboxesRepository.existsEmailMailboxesWithNameAndStatus(request.getName().trim(), List.of(ACTIVE, INACTIVE))) {
            throw new ClientException("E-Mail Mailboxes with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = emailMailboxesRepository.lastOrderingId();
        EmailMailboxes emailMailboxes = new EmailMailboxes(request);
        emailMailboxes.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        emailMailboxes.setIsHardCoded(false);

        validateChecksAdd(request, emailMailboxes);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        emailMailboxesRepository.save(emailMailboxes);

        return new EmailMailboxesResponse(emailMailboxes);
    }

    @Transactional
    public EmailMailboxesResponse edit(Long id, EmailMailboxesRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        EmailMailboxes emailMailboxes = emailMailboxesRepository.findByIdAndStatuses(id, List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mailbox not found;"));

        if (emailMailboxesRepository.existsEmailMailboxesWithNameAndStatus(request.getName().trim(), List.of(ACTIVE, INACTIVE))
                && !emailMailboxes.getName().equals(request.getName().trim())) {
            throw new ClientException("E-Mail Mailboxes with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (emailMailboxes.getIsHardCoded() && !emailMailboxes.getName().equals(request.getName())) {
            log.error("Can't edit the hardcoded nomenclature name");
            throw new OperationNotAllowedException("name-Hardcoded nomenclature name can't be changed.;");
        }

        if (emailMailboxes.getIsHardCoded() && !Objects.equals(emailMailboxes.getStatus(), request.getStatus())) {
            log.error("Can't edit the hardcoded nomenclature status");
            throw new OperationNotAllowedException("status-Hardcoded nomenclature status can't be changed.;");
        }

        validateChecksEdit(request, emailMailboxes);
        emailMailboxes.setName(request.getName().trim());
        emailMailboxes.setStatus(request.getStatus());
        emailMailboxes.setEmailAddress(request.getEmailAddress());

        return new EmailMailboxesResponse(emailMailboxes);
    }

    public Page<EmailMailboxesResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        return emailMailboxesRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        ).map(EmailMailboxesResponse::new);
    }

    public EmailMailboxesResponse view(Long id) {
        EmailMailboxes emailMailboxes = emailMailboxesRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("E-Mail Mailbox not found"));
        return new EmailMailboxesResponse(emailMailboxes);
    }


    private void checkDefaultSelectionAdd(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean defaultSelection) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setDefaultSelection(false);
        } else {
            if (defaultSelection) {
                Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByDefaultSelectionTrue();
                if (currentDefault.isPresent()) {
                    EmailMailboxes currentEmail = currentDefault.get();
                    currentEmail.setDefaultSelection(false);
                    emailMailboxesRepository.save(currentEmail);
                }
            }
            emailMailboxes.setDefaultSelection(defaultSelection);
        }
    }

    private void checkDefaultSelectionEdit(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean defaultSelection) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setDefaultSelection(false);
        } else {
            if (defaultSelection) {
                if (!emailMailboxes.isDefaultSelection()) {
                    Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByDefaultSelectionTrue();
                    if (currentDefault.isPresent()) {
                        EmailMailboxes currentEmail = currentDefault.get();
                        currentEmail.setDefaultSelection(false);
                        emailMailboxesRepository.save(currentEmail);
                    }
                }
            }
            emailMailboxes.setDefaultSelection(defaultSelection);
        }
    }

    private void checkDefaultForEmailForSendingInvoices(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean emailForSendingInvoices) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setEmailForSendingInvoices(false);
        } else {
            if (emailForSendingInvoices) {
                Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();
                if (currentDefault.isPresent()) {
                    EmailMailboxes currentEmail = currentDefault.get();
                    currentEmail.setEmailForSendingInvoices(false);
                    emailMailboxesRepository.save(currentEmail);
                }
            }
            emailMailboxes.setEmailForSendingInvoices(emailForSendingInvoices);
        }
    }

    private void checkDefaultForEmailForSendingInvoicesEdit(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean emailForSendingInvoices) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setEmailForSendingInvoices(false);
        } else {
            if (emailForSendingInvoices) {
                if (!emailMailboxes.isEmailForSendingInvoices()) {
                    Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();
                    if (currentDefault.isPresent()) {
                        EmailMailboxes currentEmail = currentDefault.get();
                        currentEmail.setEmailForSendingInvoices(false);
                        emailMailboxesRepository.save(currentEmail);
                    }
                }
            }
            emailMailboxes.setEmailForSendingInvoices(emailForSendingInvoices);
        }
    }

    private void checkDefaultForEmailForGridOperator(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean emailForGridOperator) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setEmailForGridOperator(false);
        } else {
            if (emailForGridOperator) {
                Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByEmailForGridOperatorTrue();
                if (currentDefault.isPresent()) {
                    EmailMailboxes currentEmail = currentDefault.get();
                    currentEmail.setEmailForGridOperator(false);
                    emailMailboxesRepository.save(currentEmail);
                }
            }
            emailMailboxes.setEmailForGridOperator(emailForGridOperator);
        }
    }

    private void checkDefaultForEmailForGridOperatorEdit(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean emailForGridOperator) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setEmailForGridOperator(false);
        } else {
            if (emailForGridOperator) {
                if (!emailMailboxes.isEmailForGridOperator()) {
                    Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByEmailForGridOperatorTrue();
                    if (currentDefault.isPresent()) {
                        EmailMailboxes currentEmail = currentDefault.get();
                        currentEmail.setEmailForGridOperator(false);
                        emailMailboxesRepository.save(currentEmail);
                    }
                }
            }
            emailMailboxes.setEmailForGridOperator(emailForGridOperator);
        }
    }

    private void checkDefaultForCommunicationForContract(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean communicationForContract) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setCommunicationForContract(false);
        } else {
            if (communicationForContract) {
                Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByCommunicationForContractTrue();
                if (currentDefault.isPresent()) {
                    EmailMailboxes currentEmail = currentDefault.get();
                    currentEmail.setCommunicationForContract(false);
                    emailMailboxesRepository.save(currentEmail);
                }
            }
            emailMailboxes.setCommunicationForContract(communicationForContract);
        }
    }

    private void checkDefaultForCommunicationForContractEdit(NomenclatureItemStatus status, EmailMailboxes emailMailboxes, boolean communicationForContract) {
        if (status.equals(INACTIVE)) {
            emailMailboxes.setCommunicationForContract(false);
        } else {
            if (communicationForContract) {
                if (!emailMailboxes.isCommunicationForContract()) {
                    Optional<EmailMailboxes> currentDefault = emailMailboxesRepository.findByCommunicationForContractTrue();
                    if (currentDefault.isPresent()) {
                        EmailMailboxes currentEmail = currentDefault.get();
                        currentEmail.setCommunicationForContract(false);
                        emailMailboxesRepository.save(currentEmail);
                    }
                }
            }
            emailMailboxes.setCommunicationForContract(communicationForContract);
        }
    }

    private void validateChecks(EmailMailboxesRequest request, List<String> errorMessages) {
        if (request.isEmailForSendingInvoices()) {
            if (emailMailboxesRepository.existsByEmailForSendingInvoicesAndStatus(List.of(ACTIVE, INACTIVE))) {
                errorMessages.add("emailForSendingInvoices-[emailForSendingInvoices] E-Mail Mailbox already exists with email for sending invoices;");
            }
        }
        if (request.isEmailForGridOperator()) {
            if (emailMailboxesRepository.existsByEmailForGridOperatorAndStatus(List.of(ACTIVE, INACTIVE))) {
                errorMessages.add("emailForGridOperator-[emailForGridOperator] E-Mail Mailbox already exists with email for grid operator;");
            }
        }
        if (request.isCommunicationForContract()) {
            if (emailMailboxesRepository.existsByCommunicationForContractAndStatus(List.of(ACTIVE, INACTIVE))) {
                errorMessages.add("communicationForContract-[communicationForContract] E-Mail Mailbox already exists with communication for contract;");
            }
        }
    }


    private void validateChecksAdd(EmailMailboxesRequest request, EmailMailboxes emailMailboxes) {
        checkDefaultSelectionAdd(request.getStatus(), emailMailboxes, request.getDefaultSelection());
        checkDefaultForEmailForSendingInvoices(request.getStatus(), emailMailboxes, request.isEmailForSendingInvoices());
        checkDefaultForEmailForGridOperator(request.getStatus(), emailMailboxes, request.isEmailForGridOperator());
        checkDefaultForCommunicationForContract(request.getStatus(), emailMailboxes, request.isCommunicationForContract());
    }

    private void validateChecksEdit(EmailMailboxesRequest request, EmailMailboxes emailMailboxes) {
        checkDefaultSelectionEdit(request.getStatus(), emailMailboxes, request.getDefaultSelection());
        checkDefaultForEmailForSendingInvoicesEdit(request.getStatus(), emailMailboxes, request.isEmailForSendingInvoices());
        checkDefaultForEmailForGridOperatorEdit(request.getStatus(), emailMailboxes, request.isEmailForGridOperator());
        checkDefaultForCommunicationForContractEdit(request.getStatus(), emailMailboxes, request.isCommunicationForContract());
    }

}
