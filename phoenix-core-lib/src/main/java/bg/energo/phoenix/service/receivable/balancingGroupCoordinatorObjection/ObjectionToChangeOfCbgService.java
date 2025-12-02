package bg.energo.phoenix.service.receivable.balancingGroupCoordinatorObjection;

import bg.energo.mass_comm.models.Attachment;
import bg.energo.mass_comm.models.SendEmailResponse;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.*;
import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection.*;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.*;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionToCbgFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
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
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.*;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectionToChangeOfCbgService {
    private static final String CHANGE_OF_CBG_PREFIX = "Objection-";
    private final GridOperatorRepository gridOperatorRepository;
    private final ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository;
    private final TaskService taskService;
    private final PermissionService permissionService;
    private final ObjectionToChangeOfCbgProcessService objectionToChangeOfCbgProcessService;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
    private final ObjectionToChangeOfCbgPodsRepository objectionToChangeOfCbgPodsRepository;
    private final ObjectionToChangeOfCbgProcessResultRepository objectionToChangeOfCbgProcessResultRepository;
    private final ObjectionToChangeOfCbgFileRepository objectionToChangeOfCbgFileRepository;
    private final GroundForObjectionWithdrawalToChangeOfACbgRepository groundForObjectionWithdrawalToChangeOfACbgRepository;
    private final BalancingGroupCoordinatorGroundRepository balancingGroupCoordinatorGroundRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ObjectionToCbgTemplateRepository cbgTemplateRepository;
    private final ObjectionToChangeOfCbgSubFilesRepository objectionToChangeOfCbgSubFilesRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final ObjectionOfCbgDocumentCreationService objectionOfCbgDocumentCreationService;
    private final EmailSenderService emailSenderService;
    private final ObjectionToChangeOfCbgSubFilesService subFilesService;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final DocumentsRepository documentsRepository;
    private final DocumentGenerationUtil documentGenerationUtil;


    /**
     * Creates a new objection to change of balancing group coordinator.
     *
     * @param request The request containing details for creating the objection
     * @return The ID of the created objection
     * @throws DomainEntityNotFoundException if referenced entities are not found
     * @throws ClientException               if validation fails
     */
    @Transactional
    public Long create(BalancingGroupCoordinatorObjectionRequest request) {
        log.info("Create Balancing group coordinator objection with request: {};", request);

        List<String> errorMessages = new ArrayList<>();
        ObjectionToChangeOfCbg objectionToChangeOfCbg = new ObjectionToChangeOfCbg();

        GridOperator gridOperator = gridOperatorRepository.findById(request.getGridOperatorId()).orElseThrow(
                () -> new DomainEntityNotFoundException("Grid Operator with id: %s not found;".formatted(request.getGridOperatorId()))
        );
        objectionToChangeOfCbg.setGridOperatorId(request.getGridOperatorId());
        objectionToChangeOfCbg.setChangeDate(request.getDateOfChange());
        objectionToChangeOfCbg.setStatus(EntityStatus.ACTIVE);
        validateAndSetTemplate(request.getEmailTemplateId(), objectionToChangeOfCbg);
        objectionToChangeOfCbg.setChangeOfCbgStatus(ChangeOfCbgStatus.DRAFT);
        objectionToChangeOfCbg.setChangeOfCbgNumber(CHANGE_OF_CBG_PREFIX);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        objectionToChangeOfCbgRepository.saveAndFlush(objectionToChangeOfCbg);
        objectionToChangeOfCbg.setChangeOfCbgNumber(CHANGE_OF_CBG_PREFIX + objectionToChangeOfCbg.getId());
        objectionToChangeOfCbgRepository.saveAndFlush(objectionToChangeOfCbg);

        if (request.getFileId() == null) {
            errorMessages.add("fileId-[fileId] is mandatory field;");
        }
        saveTemplates(request.getTemplateIds(), objectionToChangeOfCbg.getId(), errorMessages);
        validateFiles(request.getSubFileIds(), errorMessages, objectionToChangeOfCbg.getId());

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        FileContent content = objectionToChangeOfCbgProcessService.downloadProxyFile(request.getFileId());
        ObjectionToChangeOfCbgFile changeOfCbgFile = objectionToChangeOfCbgFileRepository.findByIdAndStatus(request.getFileId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find file with id: %s".formatted(request.getFileId())));
        changeOfCbgFile.setChangeOfCbg(objectionToChangeOfCbg.getId());
        objectionToChangeOfCbgFileRepository.save(changeOfCbgFile);
        Set<String> podIdentifiers = loadPodsFromFile(content, request.getGridOperatorId());
        objectionToChangeOfCbgProcessService.savePodsForObjectionCreate(podIdentifiers, objectionToChangeOfCbg.getId());

        objectionToChangeOfCbgProcessService.startProcess(objectionToChangeOfCbg.getId());

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return objectionToChangeOfCbg.getId();
    }

    /**
     * Test endpoint to parse a DOCX email template into plain text.
     * Downloads the document by ID and converts its content to string format.
     *
     * @param emailDocId The ID of the email template document to parse
     * @return The parsed content of the email template as a string
     * @throws IOException if there's an error downloading or parsing the document
     */
    public String parseEmailTemplateDocTest(Long emailDocId) throws IOException {
        FileContent fileContent = subFilesService.downloadDocument(emailDocId);
        return parseDocx(fileContent.getContent());

    }

    /**
     * Sends an email notification for an objection with generated documents as attachments.
     * The email includes a generated email body and additional document attachments based on templates.
     *
     * @param objectionId  The ID of the objection to generate email content for
     * @param emailAddress The recipient's email address
     * @throws RuntimeException              if there's an error during email generation or sending
     * @throws DomainEntityNotFoundException if no default grid operator email is found
     */
    public void sendEmailForObjection(Long objectionId, String emailAddress) {
        log.debug("[EMAIL_START] Starting email generation. ObjectionId: {}, Recipient: {}",
                objectionId, emailAddress);

        try {
            ObjectionDocumentGenerationResponse emailDocument = objectionOfCbgDocumentCreationService.generateDocument(
                    objectionId,
                    DocumentGenerationType.EMAIL_TEMPLATE
            );
            Long documentId = emailDocument.getEmailDocumentId().keySet().iterator().next();
            log.debug("[EMAIL_DOC] Generated document. Id: {}", documentId);

            FileContent fileContent = subFilesService.downloadDocument(documentId);
            String emailBody = parseDocx(fileContent.getContent());
            log.debug("[EMAIL_BODY] Content length: {} chars", emailBody.length());

            List<Attachment> attachments = generateDocumentFromDocumentTmpl(objectionId);
            log.debug("[EMAIL_FILES] Attachments: {}, ObjectionId: {}",
                    attachments.size(), objectionId);

            String senderEmail = emailMailboxesRepository.findDefaultGridOperatorMail()
                    .orElseThrow(() -> {
                        log.error("[EMAIL_FAIL] No default sender found. ObjectionId: {}", objectionId);
                        return new DomainEntityNotFoundException("There is no default email in database for grid operator.");
                    });

            log.info("[EMAIL_SEND] From: {}, To: {}, ObjectionId: {}",
                    senderEmail, emailAddress, objectionId);

            Optional<SendEmailResponse> sendEmailResponse = emailSenderService.sendEmailFrom(
                    emailAddress,
                    emailDocument.getEmailDocumentId().values().iterator().next(),
                    emailBody,
                    senderEmail,
                    attachments
            );

            sendEmailResponse.ifPresentOrElse(
                    response -> {
                        log.info("[EMAIL_OK] Sent. ObjectionId: {}, Status: {}, TaskId: {}",
                                objectionId,
                                response.getStatus(),
                                response.getTaskId());

                        if (log.isDebugEnabled()) {
                            log.debug("[EMAIL_RESPONSE] ObjectionId: {}, Message: {}",
                                    objectionId, response.getMessage());
                        }
                    },
                    () -> log.warn("[EMAIL_MISSING] No response received. ObjectionId: {}", objectionId)
            );

        } catch (Exception e) {
            log.error("[EMAIL_ERROR] Failed. ObjectionId: {}, To: {}, Error: {}",
                    objectionId, emailAddress, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Filters the provided set of pod identifiers based on the given grid operator ID.
     * This method returns a subset of pod identifiers that correspond to the specified grid operator.
     * If the provided set of pod identifiers is null or empty, it will return the input set as-is.
     *
     * @param gridOperatorId the unique identifier of the grid operator to filter pods by
     * @param podIdentifiers the set of pod identifiers to filter
     * @return a filtered set of pod identifiers associated with the given grid operator,
     * or the original set if it is null or empty
     */
    public Set<String> filterPodsByGridOperator(Long gridOperatorId, Set<String> podIdentifiers) {
        if (podIdentifiers == null || podIdentifiers.isEmpty()) {
            return podIdentifiers;
        }
        return objectionToChangeOfCbgPodsRepository.filterPodIdentifiersByGridOperator(gridOperatorId, podIdentifiers);
    }

    private List<Attachment> generateDocumentFromDocumentTmpl(Long objectionId) {
        return objectionOfCbgDocumentCreationService
                .generateDocument(objectionId, DocumentGenerationType.DOCUMENT_TEMPLATE)
                .getDocumentIds()
                .stream()
                .map(subFilesService::downloadDocument)
                .map(documentGenerationUtil::convertFileContentToAttachment)
                .toList();
    }

    /**
     * Updates a Balancing Group Coordinator objection with the provided data and performs
     * required validations, updates, and processing steps.
     *
     * @param id      the ID of the Balancing Group Coordinator objection to update
     * @param request the details of the update request, including template IDs, task IDs,
     *                and other relevant data
     * @return the updated ID of the Balancing Group Coordinator objection
     * @throws DomainEntityNotFoundException if the objection or grid operator with the specified ID cannot be found
     */
    @Transactional
    public Long update(Long id, BalancingGroupCoordinatorObjectionEditRequest request) {
        log.info("Update Balancing group coordinator objection with request: {};", request);

        List<String> errorMessages = new ArrayList<>();
        ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Balancing Group coordinator Objection with id: %s;".formatted(id)));
        if (!objection.getChangeOfCbgStatus().equals(ChangeOfCbgStatus.DRAFT)) {
            editFiles(objection, errorMessages, request);
        } else {
            validateAndSetTemplate(request.getEmailTemplateId(), objection);
            editFiles(objection, errorMessages, request);
            objection.setChangeDate(request.getDateOfChange());

            if (!request.getGridOperatorId().equals(objection.getGridOperatorId())) {
                removeOutdatedCbgPods(objection.getId());
                if (!gridOperatorRepository.existsByIdAndStatusIn(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                    errorMessages.add("Can't find grid operator with id: %s".formatted(request.getGridOperatorId()));
                } else {
                    objection.setGridOperatorId(request.getGridOperatorId());
                }
            }

            Set<Long> podIds = objectionToChangeOfCbgPodsRepository.findPodsByObjectionToCbgId(objection.getId());

            if (request.getFileId() != null) {
                removeOutdatedCbgPods(objection.getId());
                FileContent content = objectionToChangeOfCbgProcessService.downloadProxyFile(request.getFileId());
                Set<String> podIdentifiers = loadPodsFromFile(content, request.getGridOperatorId());
                objectionToChangeOfCbgProcessService.savePodsForObjectionEdit(podIdentifiers, podIds, objection.getId());
                objectionToChangeOfCbgProcessService.startProcess(id);
            } else if (request.getProcessEditRequest() != null) {
                processResultEdit(request.getProcessEditRequest(), objection.getId(), errorMessages);
            }
            updateTemplates(request.getTemplateIds(), objection.getId(), errorMessages);
        }
        send(request.getSaveAs(), objection);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return objection.getId();
    }

    private Set<String> loadPodsFromFile(FileContent content, Long gridOperatorId) {
        Set<String> validPodIdentifiers;
        try {
            validPodIdentifiers = objectionToChangeOfCbgProcessService.readFile(content.getContent());
        } catch (IOException e) {
            throw new ClientException("Failed to read file content;", ErrorCode.APPLICATION_ERROR);
        }
        return filterPodsByGridOperator(gridOperatorId, validPodIdentifiers);
    }

    /**
     * Removes outdated CBG pods and related process results associated with the given CBG ID.
     * This method identifies outdated CBG pod IDs and process result IDs and deletes them from
     * their respective repositories.
     *
     * @param id the ID of the change of CBG for which outdated pods and process results need to be removed
     */
    private void removeOutdatedCbgPods(Long id) {
        Set<Long> outdatedCbgPodIds = objectionToChangeOfCbgPodsRepository.findCbgPodIdsByCbgId(id);
        if (!outdatedCbgPodIds.isEmpty()) {
            objectionToChangeOfCbgPodsRepository.deleteAllById(outdatedCbgPodIds);
        }
        Set<Long> processResultIds = objectionToChangeOfCbgProcessResultRepository.findProcessResultIdsByChangeOfCbgId(id);
        if (!processResultIds.isEmpty()) {
            objectionToChangeOfCbgProcessResultRepository.deleteAllById(processResultIds);
        }
    }

    /**
     * Sends an email for the objection to the change of the balancing group if the provided status is SAVE_AND_SEND.
     * Updates the status of the objection and cleans up related draft data if the email is successfully sent.
     *
     * @param saveAs                 the status indicating whether to save and send, or only save the objection.
     * @param objectionToChangeOfCbg the objection entity containing details for the change of the balancing group.
     * @throws DomainEntityNotFoundException if the grid operator with the given ID is not found.
     * @throws ClientException               if the grid operator does not have a valid email address for objections.
     */
    private void send(ChangeOfCbgCreateStatus saveAs, ObjectionToChangeOfCbg objectionToChangeOfCbg) {
        if (saveAs != null && saveAs.equals(ChangeOfCbgCreateStatus.SAVE_AND_SEND)) {
            GridOperator gridOperator = gridOperatorRepository.findById(objectionToChangeOfCbg.getGridOperatorId()).orElseThrow(
                    () -> new DomainEntityNotFoundException("Grid Operator with id: %s not found;".formatted(objectionToChangeOfCbg.getGridOperatorId()))
            );

            if (gridOperator.getObjectionToChangeCBGEmail() == null) {
                throw new ClientException("Grid operator doesn't has objection To change of balancing group email address;", ErrorCode.APPLICATION_ERROR);
            }
            sendEmailForObjection(objectionToChangeOfCbg.getId(), gridOperator.getObjectionToChangeCBGEmail());

            objectionToChangeOfCbg.setChangeOfCbgStatus(ChangeOfCbgStatus.SEND);
            objectionToChangeOfCbgRepository.saveAndFlush(objectionToChangeOfCbg);

            Set<Long> cbgPodIds = objectionToChangeOfCbgPodsRepository.findCbgPodIdsInDraftObjections(objectionToChangeOfCbg.getId());
            objectionToChangeOfCbgPodsRepository.deleteAllById(cbgPodIds);
            Set<Long> processResultIds = objectionToChangeOfCbgProcessResultRepository.findProcessResultIdsInDraftObjections(objectionToChangeOfCbg.getId());
            objectionToChangeOfCbgProcessResultRepository.deleteAllById(processResultIds);
        }
    }

    /**
     * Processes and updates the results of objection process requests.
     *
     * @param processEditRequest List of process result edit requests
     * @param changeOfCbgId      ID of the objection
     * @param errorMessages      List to collect validation error messages
     * @throws ClientException if referenced entities not found
     */
    private void processResultEdit(List<BalancingGroupCoordinatorObjectionProcessResultEditRequest> processEditRequest, Long changeOfCbgId, List<String> errorMessages) {
        List<ObjectionToChangeOfCbgProcessResult> updatedProcessResults = new ArrayList<>();
        for (BalancingGroupCoordinatorObjectionProcessResultEditRequest processReq : processEditRequest) {
            Optional<ObjectionToChangeOfCbgProcessResult> optProcessResult = objectionToChangeOfCbgProcessResultRepository.findObjectionToChangeOfCbgProcessResultByIdAndChangeOfCbg(processReq.getProcessResultId(), changeOfCbgId);
            if (optProcessResult.isPresent()) {
                ObjectionToChangeOfCbgProcessResult processResult = optProcessResult.get();

                if (processReq.getGroundForObjectionWithdrawalToCbgId() != null) {
                    if (groundForObjectionWithdrawalToChangeOfACbgRepository.existsByIdAndStatusIn(processReq.getGroundForObjectionWithdrawalToCbgId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                        processResult.setGroundForObjWithdrawalToChangeOfCbg(processReq.getGroundForObjectionWithdrawalToCbgId());
                    } else {
                        errorMessages.add("Ground for objection withdrawal to change of cbg with id: %s, doesn't exists".formatted(processReq.getGroundForObjectionWithdrawalToCbgId()));
                    }
                }
                if (processReq.getBalancingGroupCoordinatorId() != null) {
                    if (balancingGroupCoordinatorGroundRepository.existsByIdAndStatusIn(processReq.getBalancingGroupCoordinatorId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                        processResult.setBalancingGroupCoordinatorGround(processReq.getBalancingGroupCoordinatorId());
                    } else {
                        errorMessages.add("Balancing Group Coordinator with id: %s, doesn't exists".formatted(processReq.getBalancingGroupCoordinatorId()));
                    }
                }
                processResult.setChecked(processReq.isChecked());
                updatedProcessResults.add(processResult);
            } else {
                errorMessages.add("Can't find process result with id: %s".formatted(processReq.getProcessResultId()));
            }

        }
        objectionToChangeOfCbgProcessResultRepository.saveAll(updatedProcessResults);
    }

    /**
     * Retrieves a detailed view of an objection.
     *
     * @param objectionId ID of the objection to view
     * @return Detailed response containing objection information
     * @throws DomainEntityNotFoundException if objection not found
     */
    @Transactional(readOnly = true)
    public ObjectionToChangeOfCbgResponse view(Long objectionId) {
        log.info("Previewing Customer Deposit with id: %s".formatted(objectionId));

        List<EntityStatus> objectionStatuses = new ArrayList<>();
        if (hasViewPermission()) {
            objectionStatuses.add(EntityStatus.ACTIVE);
        }
        if (hasDeletedPermission()) {
            objectionStatuses.add(EntityStatus.DELETED);
        }
        ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository.findByIdAndStatusIn(objectionId, objectionStatuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Balancing Group Coordinator Objection with id: %s".formatted(objectionId)));

        ObjectionToChangeOfCbgResponse objectionResponse = new ObjectionToChangeOfCbgResponse(objection);
        objectionResponse.setCreationDate(toLocalDate(objection.getCreateDate()));
        objectionResponse.setTemplateResponses(findTemplatesForContract(objectionId));
        contractTemplateRepository.findTemplateResponseById(objection.getEmailTemplateId(), LocalDate.now()).ifPresent(objectionResponse::setEmailTemplateResponse);

        setObjectionToCbgFiles(objection, objectionResponse);

        List<TaskShortResponse> taskShortResponses = getTasks(objectionId);
        objectionResponse.setTaskShortResponse(taskShortResponses);
        GridOperator gridOperator = gridOperatorRepository.findById(objection.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(objection.getGridOperatorId())));
        objectionResponse.setGridOperatorResponse(new GridOperatorResponse(gridOperator));

        List<ShortResponse> withdrawalShortResponse = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository
                .findByChangeOfCbgIdAndStatus(objectionId, EntityStatus.ACTIVE)
                .orElse(null);

        objectionResponse.setWithdrawalShortResponse(withdrawalShortResponse);
        return objectionResponse;
    }

    /**
     * Retrieves a list of tasks associated with a given identifier.
     *
     * @param id the unique identifier used to fetch the associated tasks
     * @return a list of TaskShortResponse objects containing details of the tasks
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByObjectionToChangeOfCbg(id);
    }

    /**
     * Retrieves and sets all associated files for an Objection to Change of CBG in the response object.
     * The method combines active sub-files specific to the objection and general documents,
     * enriching each with the responsible manager's display name.
     *
     * @param objection The objection entity containing CBG change details
     * @param response  The response object to be populated with file information
     */
    private void setObjectionToCbgFiles(ObjectionToChangeOfCbg objection, ObjectionToChangeOfCbgResponse response) {
        List<ObjectionToCbgFileResponse> files = new ArrayList<>(objectionToChangeOfCbgSubFilesRepository
                .findByObjToChangeOfCbgIdAndStatus(objection.getId(), EntityStatus.ACTIVE)
                .stream()
                .map(file -> new ObjectionToCbgFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
                .toList());

        files.addAll(documentsRepository.findDocumentsForObjectionToCbg(objection.getId())
                .stream()
                .map(file -> new ObjectionToCbgFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
                .toList());

        response.setSubFiles(files);
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


    /**
     * Retrieves a filtered and paginated list of objections.
     *
     * @param request The listing request containing filter criteria
     * @return Paginated list of objection responses
     */
    public Page<ObjectionToChangeOfCbgListingResponse> list(BalancingGroupCoordinatorObjectionListingRequest request) {
        LocalDateTime createDateFrom = null;
        LocalDateTime createDateTo = null;

        List<EntityStatus> objectionStatuses = new ArrayList<>();
        if (hasViewPermission()) {
            addStatusIfPresent(request, objectionStatuses, EntityStatus.ACTIVE);
        }
        if (hasDeletedPermission()) {
            addStatusIfPresent(request, objectionStatuses, EntityStatus.DELETED);
        }
        if (request.getCreateDateFrom() != null) {
            createDateFrom = toLocalDateTimeToMidnight(request.getCreateDateFrom());
        }
        if (request.getCreateDateTo() != null) {
            createDateTo = toLocalDateTimeToUntilMidnight(request.getCreateDateTo());
        }

        return objectionToChangeOfCbgRepository
                .filter(
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getChangeStatus()),
                        ListUtils.emptyIfNull(request.getGridOperatorIds()),
                        createDateFrom,
                        createDateTo,
                        request.getChangeDateFrom(),
                        request.getChangeDateTo(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(objectionStatuses),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        Objects.requireNonNullElse(request.getSearchBy(), ObjectionSearchBy.ALL).name(),
                        request.getNumberOfPodsFrom(),
                        request.getNumberOfPodsTo(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSearchByEnum(request.getSortBy()))
                                )
                        )

                )
                .map(ObjectionToChangeOfCbgListingResponse::new);
    }

    public ObjectionDocumentGenerationResponse createDocument(Long cbgId, DocumentGenerationType type) {
        return objectionOfCbgDocumentCreationService.generateDocument(cbgId, type);
    }


    /**
     * Filters objections for withdrawal processing.
     *
     * @param request The filter request containing search criteria
     * @return Paginated list of filtered objections
     */
    public Page<ObjectionToChangeOfCbgFilterForWithdrawalResponse> filterForWithdrawal(BalancingGroupCoordinatorObjectionFilterForWithdrawalRequest request) {
        return objectionToChangeOfCbgRepository
                .findByStatusAndChangeOfCbgNumber(
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(List.of(ChangeOfCbgStatus.SEND)),
                        List.of(EntityStatus.ACTIVE.name()),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        ObjectionSearchBy.CHANGE_OF_CBG_NUMBER.name(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), ObjectionSortingFields.NUMBER.getValue())
                                )
                        )

                )
                .map(ObjectionToChangeOfCbgFilterForWithdrawalResponse::new);
    }

    /**
     * Deletes an objection by marking it as DELETED.
     * status other than {@code ChangeOfCbgStatus.DRAFT} will result in an error.
     *
     * @param id ID of the objection to delete
     * @return The ID of the deleted objection
     * @throws DomainEntityNotFoundException if objection not found
     * @throws ClientException               if the objection's change-of-CBG status is not {@code DRAFT}
     */
    public Long delete(Long id) {
        ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Balancing Group Coordinator Objection with id: %s".formatted(id)));

        if (!ChangeOfCbgStatus.DRAFT.equals(objection.getChangeOfCbgStatus())) {
            throw new ClientException(
                    String.format("Objection with id %d cannot be deleted as its status is not DRAFT but %s.",
                            id, objection.getChangeOfCbgStatus()), ErrorCode.OPERATION_NOT_ALLOWED);
        }

        objection.setStatus(EntityStatus.DELETED);
        objectionToChangeOfCbgRepository.save(objection);
        return objection.getId();
    }

    /**
     * Adds the specified status to the list of objection statuses if it is present
     * in the entity status list of the provided request.
     *
     * @param request           the request containing the entity status list
     * @param objectionStatuses the list to which the status will be added if present
     * @param status            the specific status to check and add to the list
     */
    private void addStatusIfPresent(BalancingGroupCoordinatorObjectionListingRequest request, List<EntityStatus> objectionStatuses, EntityStatus status) {
        if (EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getEntityStatus()).contains(status.name())) {
            objectionStatuses.add(status);
        }
    }

    private LocalDate toLocalDate(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate();
    }

    private LocalDateTime toLocalDateTimeToMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.MIDNIGHT;
        return LocalDateTime.of(localDate, time);
    }

    /**
     * Converts LocalDate to LocalDateTime at end of day.
     *
     * @param localDate The date to convert
     * @return LocalDateTime at 23:59:59.999999
     */
    private LocalDateTime toLocalDateTimeToUntilMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.of(23, 59, 59, 999999);
        return LocalDateTime.of(localDate, time);
    }

    private boolean hasViewPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.BALANCING_GROUP_OBJECTION, List.of(PermissionEnum.BALANCING_GROUP_OBJECTION_VIEW));
    }

    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.BALANCING_GROUP_OBJECTION, List.of(PermissionEnum.BALANCING_GROUP_OBJECTION_VIEW_DELETED));
    }

    private String getSearchByEnum(ObjectionSortingFields sortBy) {
        return sortBy != null ? sortBy.getValue() : ObjectionSortingFields.NUMBER.getValue();
    }

    /**
     * Retrieves process results for an objection.
     *
     * @param changeOfCbgId ID of the objection
     * @return List of process result responses
     */
    public List<ObjectionToChangeOfCbgProcessResultResponse> getProcessResults(Long changeOfCbgId) {
        return objectionToChangeOfCbgProcessResultRepository.viewProcessResults(changeOfCbgId);
    }

    /**
     * Saves template associations for an objection.
     *
     * @param templateIds   Set of template IDs to associate
     * @param objectionId   ID of the objection
     * @param errorMessages List to collect validation error messages
     */
    public void saveTemplates(Set<Long> templateIds, Long objectionId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndLanguages(templateIds, ContractTemplatePurposes.OBJECTION_CHANGE_COORD, List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN),
                List.of(ContractTemplateType.DOCUMENT), ContractTemplateStatus.ACTIVE, LocalDate.now());

        List<ObjectionToChangeOfCbgTemplates> cbgTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            cbgTemplates.add(new ObjectionToChangeOfCbgTemplates(templateId, objectionId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        cbgTemplateRepository.saveAll(cbgTemplates);
    }

    /**
     * Updates template associations for an objection.
     *
     * @param templateIds         Set of template IDs to update
     * @param objectionToChangeId ID of the objection
     * @param errorMessages       List to collect validation error messages
     */
    public void updateTemplates(Set<Long> templateIds, Long objectionToChangeId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<Long, ObjectionToChangeOfCbgTemplates> templateMap = cbgTemplateRepository.findByProductDetailId(objectionToChangeId).stream().collect(Collectors.toMap(ObjectionToChangeOfCbgTemplates::getTemplateId, j -> j));
        List<ObjectionToChangeOfCbgTemplates> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            ObjectionToChangeOfCbgTemplates remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new ObjectionToChangeOfCbgTemplates(templateId, objectionToChangeId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndLanguages(templatesToCheck.keySet(), ContractTemplatePurposes.OBJECTION_CHANGE_COORD, List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN),
                List.of(ContractTemplateType.DOCUMENT), ContractTemplateStatus.ACTIVE, LocalDate.now());
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<ObjectionToChangeOfCbgTemplates> values = templateMap.values();
        for (ObjectionToChangeOfCbgTemplates value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        cbgTemplateRepository.saveAll(templatesToSave);

    }

    /**
     * Finds templates associated with an objection.
     *
     * @param productDetailId ID of the objection
     * @return List of template responses
     */
    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return cbgTemplateRepository.findForContract(productDetailId, LocalDate.now());
    }

    /**
     * Validates and sets the email template for an objection.
     *
     * @param templateId ID of template to validate
     * @param cbg        The objection to update
     * @throws DomainEntityNotFoundException if template not found or invalid
     */
    private void validateAndSetTemplate(Long templateId, ObjectionToChangeOfCbg cbg) {
        if (Objects.equals(templateId, cbg.getEmailTemplateId()))
            return;
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.OBJECTION_CHANGE_COORD, ContractTemplateType.EMAIL, LocalDate.now())) {
            throw new DomainEntityNotFoundException("emailTemplateId-Template with id %s do not exist!;".formatted(templateId));
        }
        cbg.setEmailTemplateId(templateId);
    }

    /**
     * Validates files associated with an objection.
     *
     * @param fileIds       List of file IDs to validate
     * @param errorMessages List to collect validation error messages
     * @param objId         ID of the objection
     */
    private void validateFiles(List<Long> fileIds, List<String> errorMessages, Long objId) {
        Map<Long, ObjectionToChangeOfCbgSubFiles> fileMap = objectionToChangeOfCbgSubFilesRepository.findByIdsAndStatuses(fileIds, List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ObjectionToChangeOfCbgSubFiles::getId, f -> f));
        for (Long fileId : fileIds) {
            ObjectionToChangeOfCbgSubFiles files = fileMap.get(fileId);
            if (Objects.isNull(files)) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else if (Objects.nonNull(files.getObjToChangeOfCbgId())) {
                errorMessages.add("file with id : " + fileId + " is already attached to another object;");
            } else {
                files.setObjToChangeOfCbgId(objId);
            }
        }
    }

    /**
     * Updates file associations for an objection.
     *
     * @param obj           The objection to update files for
     * @param errorMessages List to collect validation error messages
     * @param request       The edit request containing file changes
     */
    private void editFiles(ObjectionToChangeOfCbg obj, List<String> errorMessages, BalancingGroupCoordinatorObjectionEditRequest request) {
        Map<Long, ObjectionToChangeOfCbgSubFiles> fileMap = objectionToChangeOfCbgSubFilesRepository.findByIdsAndStatuses(request.getSubFileIds(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ObjectionToChangeOfCbgSubFiles::getId, f -> f));

        Set<ObjectionToChangeOfCbgSubFiles> existing = objectionToChangeOfCbgSubFilesRepository.findByObjToChangeOfCbgIdAndStatus(obj.getId(), EntityStatus.ACTIVE);

        for (Long fileId : request.getSubFileIds()) {
            ObjectionToChangeOfCbgSubFiles cbgFiles = fileMap.get(fileId);
            if (Objects.isNull(cbgFiles)) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else if (Objects.nonNull(cbgFiles.getObjToChangeOfCbgId()) && !Objects.equals(cbgFiles.getObjToChangeOfCbgId(), obj.getId())) {
                errorMessages.add("file with id : " + fileId + " is already attached to another object;");
            } else {
                cbgFiles.setObjToChangeOfCbgId(obj.getId());
            }
        }

        for (ObjectionToChangeOfCbgSubFiles existingFile : existing) {
            ObjectionToChangeOfCbgSubFiles objFiles = fileMap.get(existingFile.getId());
            if (objFiles == null) {
                existingFile.setStatus(EntityStatus.DELETED);
            }
        }
    }
}
