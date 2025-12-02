package bg.energo.phoenix.service.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.mass_comm.models.Attachment;
import bg.energo.mass_comm.models.SendEmailResponse;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BalancingGroupCoordinatorGround;
import bg.energo.phoenix.model.entity.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbg;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgCreateStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.DocumentGenerationType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator.*;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionDocumentGenerationResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgShortResponse;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListResponse;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgFileResponse;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultFullResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BalancingGroupCoordinatorGroundRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbgRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResultRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.*;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderService;
import bg.energo.phoenix.service.document.ObjectionWithdrawalDocumentGenerationService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;
import static bg.energo.phoenix.util.epb.EPBListUtils.convertEnumListIntoStringListIfNotNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorService {

    private static final String PREFIX = "Withdrawal-";
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplateRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplateRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplateRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplateRepository;
    private final ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository;
    private final ObjectionToChangeOfCbgProcessResultRepository objectionToChangeOfCbgProcessResultRepository;
    private final TaskService taskService;
    private final PermissionService permissionService;
    private final ObjectionWithdrawalToAChangeOfACbgProcessService objectionWithdrawalToAChangeOfACbgProcessService;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository processResultRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ObjectionWithdrawalToCbgTemplateRepository cbgTemplateRepository;
    private final ObjectionWithdrawalToChangeOfCbgFilesRepository objectionWithdrawalToChangeOfCbgFilesRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final GroundForObjectionWithdrawalToChangeOfACbgRepository groundForObjectionWithdrawalToChangeOfACbgRepository;
    private final BalancingGroupCoordinatorGroundRepository balancingGroupCoordinatorGroundRepository;
    private final ObjectionWithdrawalDocumentGenerationService objectionWithdrawalDocumentGenerationService;
    private final ObjWithdrawalToAChangeOfCbgFileService fileService;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final EmailSenderService emailSenderService;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final GridOperatorRepository gridOperatorRepository;
    private final DocumentsRepository documentsRepository;


    @Transactional
    public Long create(ObjectionWithdrawalToAChangeOfCBGBaseRequest request) {
        List<String> exceptionMessages = new ArrayList<>();

        ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator entity = new ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator();
        String sequenceValue = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.getNextSequenceValue();
        entity.setId(Long.valueOf(sequenceValue));
        entity.setWithdrawalChangeOfCbgNumber(PREFIX.concat(sequenceValue));
        entity.setWithdrawalToChangeOfCbgStatus(ObjectionWithdrawalToChangeOfCbgStatus.IN_PROGRESS);
        entity.setStatus(EntityStatus.ACTIVE);
        entity.setCreateDate(LocalDateTime.now());

        validateAndSetObjectionToChangeOfACbgId(request.getObjectionToChangeOfACbgId(), entity, exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        entity = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.save(entity);
        saveTemplates(request.getTemplateIds(), entity.getId(), exceptionMessages);
        validateAndSetTemplate(request.getEmailTemplateId(), entity);
        if (CollectionUtils.isNotEmpty(request.getFileIds())) {
            validateFiles(request.getFileIds(), exceptionMessages, entity.getId());
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        objectionWithdrawalToAChangeOfACbgProcessService.startProcess(request.getObjectionToChangeOfACbgId(), entity);

        send(request.getSaveAs(), entity, request.getObjectionToChangeOfACbgId());

        return entity.getId();
    }

    /**
     * Test endpoint for generating documents related to objection withdrawals.
     * This method directly forwards the document generation request to the service layer.
     *
     * @param withdrawalId The ID of the withdrawal to generate documents for
     * @param type         The type of document to generate (EMAIL_TEMPLATE or DOCUMENT_TEMPLATE)
     * @return ObjectionDocumentGenerationResponse containing the generated document IDs and metadata
     */
    public ObjectionDocumentGenerationResponse createDocument(Long withdrawalId, DocumentGenerationType type) {
        return objectionWithdrawalDocumentGenerationService.generateDocument(withdrawalId, type);
    }

    /**
     * Sends an email notification for an objection withdrawal with generated documents as attachments.
     * The email includes a generated email body and additional document attachments based on templates.
     *
     * @param withdrawalId The ID of the withdrawal to generate email content for
     * @param emailAddress The recipient's email address
     * @throws RuntimeException              if there's an error during email generation or sending
     * @throws DomainEntityNotFoundException if no default grid operator email is found
     */
    public void sendEmailForObjectionWithdrawal(Long withdrawalId, String emailAddress) {
        log.debug("[EMAIL_START] Starting email generation. WithdrawalId: {}, Recipient: {}",
                withdrawalId, emailAddress);

        try {
            ObjectionDocumentGenerationResponse emailDocument = objectionWithdrawalDocumentGenerationService.generateDocument(
                    withdrawalId,
                    DocumentGenerationType.EMAIL_TEMPLATE
            );
            Long documentId = emailDocument.getEmailDocumentId().keySet().iterator().next();
            log.debug("[EMAIL_DOC] Generated document. Id: {}", documentId);

            FileContent fileContent = fileService.downloadDocument(documentId);
            String emailBody = parseDocx(fileContent.getContent());
            log.debug("[EMAIL_BODY] Content length: {} chars", emailBody.length());

            List<Attachment> attachments = generateDocumentFromDocumentTmpl(withdrawalId);
            log.debug("[EMAIL_FILES] Attachments: {}, WithdrawalId: {}",
                    attachments.size(), withdrawalId);

            String senderEmail = emailMailboxesRepository.findDefaultGridOperatorMail()
                    .orElseThrow(() -> {
                        log.error("[EMAIL_FAIL] No default sender found. WithdrawalId: {}", withdrawalId);
                        return new DomainEntityNotFoundException("There is no default email in database for grid operator.");
                    });

            log.info("[EMAIL_SEND] From: {}, To: {}, WithdrawalId: {}",
                    senderEmail, emailAddress, withdrawalId);

            Optional<SendEmailResponse> sendEmailResponse = emailSenderService.sendEmailFrom(
                    emailAddress,
                    emailDocument.getEmailDocumentId().values().iterator().next(),
                    emailBody,
                    senderEmail,
                    attachments
            );

            sendEmailResponse.ifPresentOrElse(
                    response -> {
                        log.info("[EMAIL_OK] Sent. WithdrawalId: {}, Status: {}, TaskId: {}",
                                withdrawalId,
                                response.getStatus(),
                                response.getTaskId());

                        if (log.isDebugEnabled()) {
                            log.debug("[EMAIL_RESPONSE] WithdrawalId: {}, Message: {}",
                                    withdrawalId, response.getMessage());
                        }
                    },
                    () -> log.warn("[EMAIL_MISSING] No response received. WithdrawalId: {}", withdrawalId)
            );

        } catch (IOException e) {
            log.error("[EMAIL_ERROR] Failed. WithdrawalId: {}, To: {}, Error: {}",
                    withdrawalId, emailAddress, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates document attachments from templates for an objection withdrawal.
     * Converts the generated documents into email attachments.
     *
     * @param withdrawalId The ID of the withdrawal to generate documents for
     * @return List of generated attachments
     */
    private List<Attachment> generateDocumentFromDocumentTmpl(Long withdrawalId) {
        return objectionWithdrawalDocumentGenerationService
                .generateDocument(withdrawalId, DocumentGenerationType.DOCUMENT_TEMPLATE)
                .getDocumentIds()
                .stream()
                .map(fileService::downloadDocument)
                .map(documentGenerationUtil::convertFileContentToAttachment)
                .toList();
    }

    private void validateAndSetObjectionToChangeOfACbgId(Long objectionToChangeOfCbgId, ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator objectionWithdrawalToAChangeOfABalancingGroupCoordinator, List<String> exceptionMessages) {
        if (objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.existsByChangeOfCbgId(objectionToChangeOfCbgId)) {
            if (objectionToChangeOfCbgProcessResultRepository.existsByChangeOfCbgAndIsChecked(objectionToChangeOfCbgId, true)) {
                objectionWithdrawalToAChangeOfABalancingGroupCoordinator.setChangeOfCbgId(objectionToChangeOfCbgId);
            } else {
                exceptionMessages.add("objectionToChangeOfACbgId-Objection to change of a cbg with id [%s] is not checked in process of objection to change of a cbg;".formatted(objectionToChangeOfCbgId));
            }
        } else {
            Optional<ObjectionToChangeOfCbg> objectionToChangeOfCbg = objectionToChangeOfCbgRepository.findByIdAndChangeOfCbgStatusInAndStatusIn(objectionToChangeOfCbgId, List.of(ChangeOfCbgStatus.SEND), List.of(EntityStatus.ACTIVE));
            if (objectionToChangeOfCbg.isEmpty()) {
                exceptionMessages.add("objectionToChangeOfACbgId-Objection to change of a cbg does not exists with given id [%s];".formatted(objectionToChangeOfCbgId));
            } else {
                objectionWithdrawalToAChangeOfABalancingGroupCoordinator.setChangeOfCbgId(objectionToChangeOfCbgId);
            }
        }
    }

    @Transactional
    public Long edit(Long id, ObjectionWithdrawalToAChangeOfCBGEditRequest request) {
        ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator entity = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Objection withdrawal to a change of a balancing group does not exists with given id [%s];".formatted(id)));

        if (entity.getStatus() == EntityStatus.DELETED) {
            throw new ClientException("Objection withdrawal to a change of a balancing group is DELETED, so you can't edit it;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        List<String> exceptionMessages = new ArrayList<>();

        if (entity.getWithdrawalToChangeOfCbgStatus() != ObjectionWithdrawalToChangeOfCbgStatus.DRAFT) {
            editFiles(entity, exceptionMessages, request);
        } else {
            if (!Objects.equals(request.getObjectionToChangeOfACbgId(), entity.getChangeOfCbgId())) {
                validateAndSetObjectionToChangeOfACbgId(request.getObjectionToChangeOfACbgId(), entity, exceptionMessages);
            }

            updateTemplates(request.getTemplateIds(), entity.getId(), exceptionMessages);
            validateAndSetTemplate(request.getEmailTemplateId(), entity);
            editFiles(entity, exceptionMessages, request);
            if (request.getProcessResultChangeRequests() != null && !request.getProcessResultChangeRequests().isEmpty()) {
                changeProcessResults(entity, request, exceptionMessages);
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.save(entity);

        send(request.getSaveAs(), entity, request.getObjectionToChangeOfACbgId());

        return id;
    }

    private void send(ChangeOfCbgCreateStatus saveAs, ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator entity, Long changeOfCbgId) {
        if (saveAs != null && saveAs.equals(ChangeOfCbgCreateStatus.SAVE_AND_SEND)) {
            ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository.findById(changeOfCbgId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find objection with id: %s".formatted(changeOfCbgId)));
            GridOperator gridOperator = gridOperatorRepository.findById(objection.getGridOperatorId()).orElseThrow(
                    () -> new DomainEntityNotFoundException("Grid Operator with id: %s not found;".formatted(objection.getGridOperatorId()))
            );
            if (gridOperator.getObjectionToChangeCBGEmail() == null) {
                throw new ClientException("Grid operator doesn't has objection To change of balancing group email address;", ErrorCode.APPLICATION_ERROR);
            }
            log.info("GridOperatorMailForWithdrawal: %s".formatted(gridOperator.getObjectionToChangeCBGEmail()));
            sendEmailForObjectionWithdrawal(entity.getId(), gridOperator.getObjectionToChangeCBGEmail());

            entity.setWithdrawalToChangeOfCbgStatus(ObjectionWithdrawalToChangeOfCbgStatus.SEND);
            objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.saveAndFlush(entity);
        }
    }


    private void changeProcessResults(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator withdrawal, ObjectionWithdrawalToAChangeOfCBGEditRequest request, List<String> errorMessages) {
        List<Long> processResultIds = request.getProcessResultChangeRequests().stream().map(ProcessResultChangeRequest::getProcessResultId)
                .toList();
        List<Long> groundIds = request.getProcessResultChangeRequests().stream().map(ProcessResultChangeRequest::getGroundsForObjectionWithdrawalId)
                .toList();
        List<Long> balancingGroupCoordinatorGroundIds = request.getProcessResultChangeRequests().stream().map(ProcessResultChangeRequest::getBalancingGroupCoordinatorGroundsId)
                .toList();

        validateForDuplicates(processResultIds, errorMessages, "process result");
        Map<Long, ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> processResultMap =
                processResultRepository.findByIdsIn(processResultIds, withdrawal.getId())
                        .stream().collect(Collectors.toMap(ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult::getId, y -> y));

        for (Long processResultId : processResultIds) {
            if (!processResultMap.containsKey(processResultId)) {
                errorMessages.add("Process result with id %s doesn't exist!;".formatted(processResultId));
            } else {
                validateProcessedPods(processResultMap.get(processResultId).getPodId(), errorMessages);
            }
        }


        Map<Long, BalancingGroupCoordinatorGround> balancingGroupCoordinatorGroundMap = balancingGroupCoordinatorGroundRepository.findByIdsIn(balancingGroupCoordinatorGroundIds)
                .stream().collect(Collectors.toMap(BalancingGroupCoordinatorGround::getId, y -> y));

        Map<Long, GroundForObjectionWithdrawalToChangeOfACbg> groundMap = groundForObjectionWithdrawalToChangeOfACbgRepository.findByIdsIn(groundIds)
                .stream().collect(Collectors.toMap(GroundForObjectionWithdrawalToChangeOfACbg::getId, y -> y));

        for (ProcessResultChangeRequest processResultChangeRequest : request.getProcessResultChangeRequests()) {
            ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult processResult = processResultMap.get(processResultChangeRequest.getProcessResultId());
            if (processResult != null) {
                if (!processResultChangeRequest.getBalancingGroupCoordinatorGroundsId().equals(processResult.getBalancingGroupCoordinatorGroundId())) {
                    if (!processResultChangeRequest.isCheck()) {
                        errorMessages.add("Unable to change balancing group coordinator ground for process result with id %s,because it's not checked!;".formatted(processResult.getId()));
                    }
                    BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundMap.get(processResultChangeRequest.getBalancingGroupCoordinatorGroundsId());
                    if (balancingGroupCoordinatorGround == null || !balancingGroupCoordinatorGround.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                        errorMessages.add("Balancing group coordinator ground with id %s doesn't exist or is not ACTIVE!;".formatted(processResultChangeRequest.getBalancingGroupCoordinatorGroundsId()));
                    } else {
                        processResult.setBalancingGroupCoordinatorGroundId(processResultChangeRequest.getBalancingGroupCoordinatorGroundsId());
                    }
                }
                if (!processResultChangeRequest.getGroundsForObjectionWithdrawalId().equals(processResult.getGroundForObjectionWithdrawalToChangeOfCbgId())) {
                    if (!processResultChangeRequest.isCheck()) {
                        errorMessages.add("Unable to change grounds for objection withdrawal for process result with id %s,because it's not checked!;".formatted(processResult.getId()));
                    }
                    GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundMap.get(processResultChangeRequest.getGroundsForObjectionWithdrawalId());
                    if (groundForObjectionWithdrawalToChangeOfACbg == null || !groundForObjectionWithdrawalToChangeOfACbg.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                        errorMessages.add("Ground for objection withdrawal with id %s doesn't exist or is not ACTIVE!;".formatted(processResultChangeRequest.getGroundsForObjectionWithdrawalId()));
                    } else {
                        processResult.setGroundForObjectionWithdrawalToChangeOfCbgId(processResultChangeRequest.getGroundsForObjectionWithdrawalId());
                    }
                }
                if (!processResult.getIsChecked().equals(processResultChangeRequest.isCheck())) {
                    if (processResult.getOverdueAmountForPod().compareTo(BigDecimal.ZERO) == 0) {
                        errorMessages.add("Process result with id %s has overdue amount for pod and can't be unchecked!;".formatted(processResult.getId()));
                    } else {
                        processResult.setIsChecked(processResultChangeRequest.isCheck());
                    }
                }
            }
        }
    }

    private void validateProcessedPods(Long podId, List<String> errorMessages) {
        List<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> checkedSentWithdrawalPods = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.findCheckedAndSentWithdrawalPodByPodId(podId);
        if (!checkedSentWithdrawalPods.isEmpty()) {
            errorMessages.add("Unable to change process result for pod with id %s, because it's already used in another objection withdrawal (id: %s, status: %s);"
                    .formatted(podId, checkedSentWithdrawalPods.get(0).getChangeWithdrawalOfCbgId(), ObjectionWithdrawalToChangeOfCbgStatus.SEND));
        }
    }

    private void validateForDuplicates(List<Long> ids, List<String> errorMessages, String name) {
        Set<Long> idSet = new HashSet<>();
        for (Long id : ids) {
            if (!idSet.contains(id)) {
                idSet.add(id);
            } else {
                errorMessages.add("Duplicate id for %s : %s;".formatted(name, id));
            }
        }
    }

    public ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse preview(Long id) {
        ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator entity = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Objection withdrawal to a change of a balancing group does not exists with given id [%s];".formatted(id))
                );

        if (entity.getStatus() == EntityStatus.ACTIVE && !hasPermission(PermissionEnum.OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW)) {
            throw new ClientException("You don't have permission to view active objection withdrawal to a change of balancing group coordinator;", ErrorCode.ACCESS_DENIED);
        } else if (entity.getStatus() == EntityStatus.DELETED && !hasPermission(PermissionEnum.OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW_DELETE)) {
            throw new ClientException("You don't have permission to view deleted objection withdrawal to a change of balancing group coordinator;", ErrorCode.ACCESS_DENIED);
        }

        ObjectionToChangeOfCbg objectionToChangeOfCbg = objectionToChangeOfCbgRepository
                .findById(entity.getChangeOfCbgId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Objection to a change of a balancing group does not exists with given id [%s];".formatted(entity.getChangeOfCbgId()))
                );

        List<TaskShortResponse> tasks = getTasks(entity.getId());

        ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse build = ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse
                .builder()
                .withId(entity.getId())
                .withNumber(entity.getWithdrawalChangeOfCbgNumber())
                .withCreationDate(entity.getCreateDate().toLocalDate())
                .withObjectionWithdrawalToChangeOfCbgStatus(entity.getWithdrawalToChangeOfCbgStatus())
                .withObjectionToChangeOfCbg(new ObjectionToChangeOfCbgShortResponse(objectionToChangeOfCbg))
                .withTasks(tasks.isEmpty() ? null : tasks)
                .withStatus(entity.getStatus())
                .withTemplateResponses(findTemplatesForContract(entity.getId()))
                .build();

        setObjectionWithdrawalFiles(entity.getId(), build);

        contractTemplateRepository
                .findTemplateResponseById(entity.getEmailTemplateId(), LocalDate.now())
                .ifPresent(build::setEmailTemplateResponse);

        return build;
    }

    /**
     * Retrieves a list of tasks associated with the specified ID.
     *
     * @param id the identifier used to fetch tasks related to objection withdrawal to change of CBG
     * @return a list of TaskShortResponse objects representing the tasks
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByObjectionWithdrawalToChangeOfCbg(id);
    }

    /**
     * Sets both uploaded and generated files for an objection withdrawal request in the response.
     * Retrieves active files from ObjectionWithdrawalToChangeOfCbgFiles and Documents repositories.
     *
     * @param withdrawalId The withdrawal entity ID for file lookup
     * @param response     The response object to set files on
     */
    private void setObjectionWithdrawalFiles(Long withdrawalId, ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorResponse response) {
        List<ObjectionWithdrawalToCbgFileResponse> files = new ArrayList<>(objectionWithdrawalToChangeOfCbgFilesRepository
                .findByObjWithdrawalToChangeOfCbgIdAndStatus(withdrawalId, EntityStatus.ACTIVE)
                .stream()
                .map(file -> new ObjectionWithdrawalToCbgFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
                .toList());

        files.addAll(documentsRepository.findDocumentsForObjWithdrawal(withdrawalId)
                .stream()
                .map(file -> new ObjectionWithdrawalToCbgFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
                .toList());

        response.setFiles(files);
    }


    /**
     * Retrieves and formats the display name of an account manager.
     * Returns the display name in format " (DisplayName)" or empty string if manager not found.
     *
     * @param userId The system user ID to look up
     * @return Formatted display name string or empty string if user not found
     */
    private String getManagerDisplayName(String userId) {
        return accountManagerRepository.findByUserName(userId)
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")"))
                .orElse("");
    }

    public Page<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListResponse> list(ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorListingRequest listingRequest) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (hasPermission(PermissionEnum.OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW_DELETE)) {
            entityStatuses.add(EntityStatus.DELETED);
        }

        if (hasPermission(PermissionEnum.OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW)) {
            entityStatuses.add(EntityStatus.ACTIVE);
        }

        return objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.list(
                        getSearchByEnum(listingRequest.getSearchFields()),
                        EPBStringUtils.fromPromptToQueryParameter(listingRequest.getPrompt()),
                        convertEnumListIntoStringListIfNotNull(entityStatuses),
                        convertEnumListIntoStringListIfNotNull(listingRequest.getWithdrawalToChangeOfCbgStatuses()),
                        listingRequest.getCreateDateFrom(),
                        listingRequest.getCreateDateTo(),
                        listingRequest.getNumberOfPodsFrom(),
                        listingRequest.getNumberOfPodsTo(),
                        PageRequest.of(
                                listingRequest.getPage(),
                                listingRequest.getSize(),
                                Sort.by(
                                        new Sort.Order(listingRequest.getDirection(), getSorByEnum(listingRequest.getColumns()))
                                )
                        )
                )
                .map(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListResponse::new);
    }


    public Long delete(Long id) {
        ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator objectionWithdrawalToAChangeOfABalancingGroupCoordinator = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Objection withdrawal to a change of a balancing group does not exists with given id [%s];".formatted(id)));

        if (objectionWithdrawalToAChangeOfABalancingGroupCoordinator.getStatus() == EntityStatus.DELETED || objectionWithdrawalToAChangeOfABalancingGroupCoordinator.getWithdrawalToChangeOfCbgStatus() == ObjectionWithdrawalToChangeOfCbgStatus.SEND) {
            throw new ClientException("You can’t delete Objection withdrawal to change of a CBG because It is connected with Objection withdrawal Withdrawal to change of a CBG;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        objectionWithdrawalToAChangeOfABalancingGroupCoordinator.setStatus(EntityStatus.DELETED);
        objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.save(objectionWithdrawalToAChangeOfABalancingGroupCoordinator);

        Optional<List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplate>> objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplatesOptional = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplateRepository.findAllByWithdrawalChangeOfCbgIdAndStatusIn(id, List.of(ReceivableSubObjectStatus.ACTIVE));
        if (objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplatesOptional.isPresent()) {
            List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplate> objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplates = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplatesOptional.get();
            objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplates.forEach(it -> {
                it.setStatus(ReceivableSubObjectStatus.DELETED);
                objectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplateRepository.save(it);
            });
        }

        Optional<List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplate>> objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplatesOptional = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplateRepository.findAllByWithdrawalChangeOfCbgIdAndStatusIn(id, List.of(ReceivableSubObjectStatus.ACTIVE));
        if (objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplatesOptional.isPresent()) {
            List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplate> objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplates = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplatesOptional.get();
            objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplates.forEach(it -> {
                it.setStatus(ReceivableSubObjectStatus.DELETED);
                objectionWithdrawalToAChangeOfABalancingGroupCoordinatorEmailTemplateRepository.save(it);
            });
        }

        return id;
    }

    private boolean hasPermission(PermissionEnum permission) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR, List.of(permission));
    }

    private String getSearchByEnum(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields objectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields) {
        return objectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields != null ? objectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields.getValue() : ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields.ALL.getValue();
    }

    private String getSorByEnum(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns objectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns) {
        return objectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns != null ? objectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns.getValue() : ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns.WITHDRAWAL_CHANGE_OF_CBG_NUMBER.getValue();
    }

    public Page<ProcessResultFullResponse> getProcessResults(Long withdrawalId, int page, int pageSize) {
        return processResultRepository.viewProcessResults(withdrawalId, PageRequest.of(page, pageSize));
    }


    public void saveTemplates(Set<Long> templateIds, Long productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templateIds, ContractTemplatePurposes.OBJECTION_WITHDRAW_CHANGE_COORD, ContractTemplateStatus.ACTIVE);

        List<ObjectionWithdrawalToCbgTemplates> cbgTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            cbgTemplates.add(new ObjectionWithdrawalToCbgTemplates(templateId, productDetailId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        cbgTemplateRepository.saveAll(cbgTemplates);
    }

    public void updateTemplates(Set<Long> templateIds, Long objectionToChangeWithdrawalId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<Long, ObjectionWithdrawalToCbgTemplates> templateMap = cbgTemplateRepository.findByProductDetailId(objectionToChangeWithdrawalId).stream().collect(Collectors.toMap(ObjectionWithdrawalToCbgTemplates::getTemplateId, j -> j));
        List<ObjectionWithdrawalToCbgTemplates> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            ObjectionWithdrawalToCbgTemplates remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new ObjectionWithdrawalToCbgTemplates(templateId, objectionToChangeWithdrawalId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.OBJECTION_WITHDRAW_CHANGE_COORD, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<ObjectionWithdrawalToCbgTemplates> values = templateMap.values();
        for (ObjectionWithdrawalToCbgTemplates value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        cbgTemplateRepository.saveAll(templatesToSave);

    }

    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return cbgTemplateRepository.findForContract(productDetailId, LocalDate.now());
    }

    private void validateAndSetTemplate(Long templateId, ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator object) {
        if (Objects.equals(templateId, object.getEmailTemplateId()))
            return;
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.OBJECTION_WITHDRAW_CHANGE_COORD, ContractTemplateType.EMAIL, LocalDate.now())) {
            throw new DomainEntityNotFoundException("emailTemplateId-Template with id %s do not exist!;".formatted(templateId));
        }
        object.setEmailTemplateId(templateId);
    }

    private void validateFiles(List<Long> fileIds, List<String> errorMessages, Long objId) {
        Map<Long, ObjectionWithdrawalToChangeOfCbgFiles> fileMap = objectionWithdrawalToChangeOfCbgFilesRepository.findByIdsAndStatuses(fileIds, List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ObjectionWithdrawalToChangeOfCbgFiles::getId, f -> f));
        for (Long fileId : fileIds) {
            ObjectionWithdrawalToChangeOfCbgFiles files = fileMap.get(fileId);
            if (Objects.isNull(files)) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else if (Objects.nonNull(files.getObjWithdrawalToChangeOfCbgId())) {
                errorMessages.add("file with id : " + fileId + " is already attached to another object;");
            } else {
                files.setObjWithdrawalToChangeOfCbgId(objId);
            }
        }
    }

    private void editFiles(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator obj, List<String> errorMessages, ObjectionWithdrawalToAChangeOfCBGEditRequest request) {
        Map<Long, ObjectionWithdrawalToChangeOfCbgFiles> fileMap = objectionWithdrawalToChangeOfCbgFilesRepository.findByIdsAndStatuses(request.getFileIds(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ObjectionWithdrawalToChangeOfCbgFiles::getId, f -> f));

        Set<ObjectionWithdrawalToChangeOfCbgFiles> existing = objectionWithdrawalToChangeOfCbgFilesRepository.findByObjWithdrawalToChangeOfCbgIdAndStatus(obj.getId(), EntityStatus.ACTIVE);

        for (Long fileId : request.getFileIds()) {
            ObjectionWithdrawalToChangeOfCbgFiles cbgFiles = fileMap.get(fileId);
            if (Objects.isNull(cbgFiles)) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else if (Objects.nonNull(cbgFiles.getObjWithdrawalToChangeOfCbgId()) && !Objects.equals(cbgFiles.getObjWithdrawalToChangeOfCbgId(), obj.getId())) {
                errorMessages.add("file with id : " + fileId + " is already attached to another object;");
            } else {
                cbgFiles.setObjWithdrawalToChangeOfCbgId(obj.getId());
            }
        }

        for (ObjectionWithdrawalToChangeOfCbgFiles existingFile : existing) {
            ObjectionWithdrawalToChangeOfCbgFiles objFiles = fileMap.get(existingFile.getId());
            if (objFiles == null) {
                existingFile.setStatus(EntityStatus.DELETED);
            }
        }
    }
}
