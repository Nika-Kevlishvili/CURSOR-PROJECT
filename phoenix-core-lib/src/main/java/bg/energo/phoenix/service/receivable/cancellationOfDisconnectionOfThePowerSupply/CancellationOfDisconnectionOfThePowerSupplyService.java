package bg.energo.phoenix.service.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationDcnTemplate;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFiles;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationPods;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyEditRequest;
import bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyListingRequest;
import bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfThePowerSupplyRequest;
import bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationPodChangeRequest;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.ReasonForCancellationRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDcnTemplateRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFileRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationPodsRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.CancellationDcnOfPwsDocumentCreationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBListingUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationOfDisconnectionOfThePowerSupplyService {
    private final CancellationOfDisconnectionOfThePowerSupplyRepository cancellationOfDisconnectionOfThePowerSupplyRepository;
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final PowerSupplyDcnCancellationFileRepository powerSupplyDcnCancellationFileRepository;
    private final PowerSupplyDcnCancellationPodsRepository powerSupplyDcnCancellationPodsRepository;
    private final CancellationDcnOfPwsDocumentCreationService documentCreationService;
    private final ReasonForCancellationRepository reasonForCancellationRepository;
    private final CancellationOfDcnTemplateRepository requestTemplatesRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final FileArchivationService fileArchivationService;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final DocumentsRepository documentsRepository;
    private final PermissionService permissionService;
    private final TaskRepository taskRepository;
    private final FileService fileService;
    private final TaskService taskService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private final static String CANCELLATION_PREFIX = "Cancellation-";

    /**
     * Creates a new cancellation of disconnection power supply request.
     *
     * @param request The CancellationOfThePowerSupplyRequest containing details for the cancellation
     * @return The ID of the created cancellation request
     * @throws ClientException               if the disconnection request doesn't exist or user lacks permissions
     * @throws DomainEntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public Long create(CancellationOfThePowerSupplyRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (!disconnectionPowerSupplyRequestRepository.existsByIdAndStatus(request.getRequestForDisconnectionOfThePowerSupplyId(), EntityStatus.ACTIVE)) {
            errorMessages.add("requestForDisconnectionOfThePowerSupplyId-[requestForDisconnectionOfThePowerSupplyId] not found;");
        }
        validatePermissions(request);
        CancellationOfDisconnectionOfThePowerSupply cancellation = new CancellationOfDisconnectionOfThePowerSupply();
        cancellation.setRequestForDisconnectionOfThePowerSupplyId(request.getRequestForDisconnectionOfThePowerSupplyId());
        cancellation.setNumber(CANCELLATION_PREFIX);
        cancellation.setCancellationStatus(request.getSaveAs());
        cancellation.setEntityStatus(EntityStatus.ACTIVE);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        cancellationOfDisconnectionOfThePowerSupplyRepository.saveAndFlush(cancellation);
        validateFiles(request.getFileIds(), cancellation, errorMessages);

        validateTable(request, cancellation, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        cancellation.setNumber(CANCELLATION_PREFIX + cancellation.getId());
        cancellationOfDisconnectionOfThePowerSupplyRepository.saveAndFlush(cancellation);
        saveTemplates(request.getTemplateIds(), cancellation.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        archiveFiles(cancellation);
        generateDocumentIfExecuted(request, cancellation);

        return cancellation.getId();
    }

    /**
     * Generates document if cancellation request has EXECUTED status
     *
     * @param request      Cancellation request containing status
     * @param cancellation Entity containing ID for document generation
     */
    private void generateDocumentIfExecuted(
            CancellationOfThePowerSupplyRequest request,
            CancellationOfDisconnectionOfThePowerSupply cancellation
    ) {
        if (isExecutionStatus(request.getSaveAs())) {
            documentCreationService.generateDocument(cancellation.getId());
        }
    }

    /**
     * Checks if status equals EXECUTED
     *
     * @param status Status to check
     * @return true if status is EXECUTED
     */
    private boolean isExecutionStatus(CancellationOfDisconnectionOfThePowerSupplyStatus status) {
        return status == CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED;
    }

    /**
     * Archives files associated with a cancellation request to EDMS.
     *
     * @param cancellation The cancellation request whose files need archiving
     */
    private void archiveFiles(CancellationOfDisconnectionOfThePowerSupply cancellation) {
        Set<PowerSupplyDcnCancellationFiles> cancellationFiles = powerSupplyDcnCancellationFileRepository.findByCancellationIdAndSubObjectStatus(cancellation.getId(), EntityStatus.ACTIVE);

        if (CollectionUtils.isNotEmpty(cancellationFiles)) {
            for (PowerSupplyDcnCancellationFiles cancellationFile : cancellationFiles) {
                try {
                    cancellationFile.setNeedArchive(true);
                    cancellationFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_CANCELLATION_OF_DISCONNECTION_FILE);
                    cancellationFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_CANCELLATION_OF_DISCONNECTION_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), cancellation.getNumber()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(cancellationFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: [%s]".formatted(cancellationFile.getLocalFileUrl()));
                }
            }
        }
    }

    /**
     * Validates user permissions based on the cancellation status.
     *
     * @param request The cancellation request to check permissions for
     * @throws ClientException if user lacks required permissions for the operation
     */
    private void validatePermissions(CancellationOfThePowerSupplyRequest request) {
        if (request.getSaveAs().equals(CancellationOfDisconnectionOfThePowerSupplyStatus.DRAFT)) {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_DRAFT);
        } else {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_EXECUTE);
        }
    }

    /**
     * Validates uploaded files for the cancellation request and associates them.
     *
     * @param fileIds       Set of file IDs to validate
     * @param errorMessages List to collect validation error messages
     * @param cancellation  The cancellation entity to associate files with
     * @throws ClientException if files are not found or invalid
     */
    public void validateFiles(
            Set<Long> fileIds,
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            List<String> errorMessages
    ) {
        Map<Long, PowerSupplyDcnCancellationFiles> fileMap = powerSupplyDcnCancellationFileRepository.findByIdsAndStatuses(fileIds, List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(PowerSupplyDcnCancellationFiles::getId, f -> f));
        for (Long fileId : fileIds) {
            PowerSupplyDcnCancellationFiles powerSupplyDcnCancellationFiles = fileMap.get(fileId);
            if (powerSupplyDcnCancellationFiles == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                powerSupplyDcnCancellationFiles.setPowerSupplyDcnCancellationId(cancellation.getId());
            }
        }
    }

    public List<Long> generateDocument(Long cancellationId) {
        return documentCreationService.generateDocument(cancellationId);
    }

    /**
     * Validates the cancellation request table data containing POD information.
     *
     * @param request       The cancellation request containing POD data
     * @param errorMessages List to collect validation error messages
     * @param cancellation  The cancellation entity to associate validated PODs with
     * @throws ClientException if POD data is invalid or missing required fields
     */
    public void validateTable(
            CancellationOfThePowerSupplyRequest request,
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            List<String> errorMessages
    ) {
        Set<CancellationPodQueryResponse> table = cancellationOfDisconnectionOfThePowerSupplyRepository
                .findForCheck(request.getRequestForDisconnectionOfThePowerSupplyId())
                .stream()
                .map(CancellationPodQueryResponse::new)
                .collect(Collectors.toSet());

        Set<CancellationPodQueryResponse> checkedTable = table.stream().filter(CancellationPodQueryResponse::getChecked).collect(Collectors.toSet());

        if (table.isEmpty()) {
            throw new ClientException("Request for disconnection with id %s doesn't has pods which can be cancelled;".formatted(request.getRequestForDisconnectionOfThePowerSupplyId()), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        for (CancellationPodQueryResponse queryResponse : checkedTable) {
            if (!request.getTable().contains(queryResponse)) {
                errorMessages.add(queryResponse + " can't be unchecked");
            }
        }
        for (CancellationPodRequest podRequest : request.getTable()) {
            if (podRequest.getCancellationReasonId() == null) {
                errorMessages.add("cancellation reason is mandatory!;");
            }
            if (!table.contains(podRequest)) {
                errorMessages.add("Invalid input" + podRequest + ";");
            }
        }

        Set<Long> requestCancellationIds = request.getTable().stream().map(CancellationPodRequest::getCancellationReasonId).collect(Collectors.toSet());
        Set<Long> fetched = reasonForCancellationRepository.findByIdsIn(requestCancellationIds, List.of(NomenclatureItemStatus.ACTIVE));
        for (Long i : requestCancellationIds) {
            if (!fetched.contains(i)) {
                errorMessages.add("Reason For Cancellation not found with id " + i + ";");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        List<PowerSupplyDcnCancellationPods> cancellationDetailedToSave = request
                .getTable()
                .stream()
                .map(podRequest -> new PowerSupplyDcnCancellationPods(podRequest, cancellation.getId()))
                .toList();
        powerSupplyDcnCancellationPodsRepository.saveAll(cancellationDetailedToSave);
    }

    /**
     * Deletes a cancellation of disconnection request.
     *
     * @param id ID of the cancellation request to delete
     * @return The ID of the deleted cancellation request
     * @throws DomainEntityNotFoundException if request not found
     * @throws OperationNotAllowedException  if request cannot be deleted
     */
    @Transactional
    public Long delete(Long id) {
        log.info("Deleting cancellation of disconnection of the power supply with id {}", id);

        CancellationOfDisconnectionOfThePowerSupply entity = getCancellationOfDisconnectionOfThePowerSupply(id);
        checkOnDeletePermissionByStatuses(entity.getEntityStatus(), entity.getCancellationStatus(), entity.getId());

        entity.setEntityStatus(EntityStatus.DELETED);
        cancellationOfDisconnectionOfThePowerSupplyRepository.save(entity);
        return id;
    }

    /**
     * Updates an existing cancellation of disconnection request.
     *
     * @param id      ID of the cancellation request to update
     * @param request The edit request containing updated information
     * @return The ID of the updated cancellation request
     * @throws ClientException               if validation fails or user lacks permissions
     * @throws DomainEntityNotFoundException if cancellation request not found
     */
    @Transactional
    public Long update(Long id, CancellationOfDisconnectionOfThePowerSupplyEditRequest request) {
        log.info("Updating cancellation of disconnection of the power supply with id: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();
        CancellationOfDisconnectionOfThePowerSupply cancellation = getCancellationOfDisconnectionOfThePowerSupply(id);
        checkOnEditPermissionByStatuses(cancellation.getEntityStatus(), cancellation.getCancellationStatus(), cancellation.getId());

        if (CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED.equals(cancellation.getCancellationStatus())) {
            editForExecuted(cancellation, request, errorMessages);
            return cancellation.getId();
        }

        editForDraft(cancellation, request, errorMessages);
        cancellation.setCancellationStatus(request.getSaveAs());
        editFiles(request, cancellation, errorMessages);
        updateTemplates(request.getTemplateIds(), cancellation.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        archiveFiles(cancellation);
        return cancellation.getId();
    }

    /**
     * Processes updates for cancellation requests in EXECUTED status.
     *
     * @param cancellation  The cancellation entity to update
     * @param errorMessages List to collect validation error messages
     * @param request       The edit request containing changes
     * @throws ClientException if attempting invalid modifications for executed status
     */
    private void editForExecuted(
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            CancellationOfDisconnectionOfThePowerSupplyEditRequest request,
            List<String> errorMessages
    ) {
        if (!request.getRequestForDisconnectionOfThePowerSupplyId().equals(cancellation.getRequestForDisconnectionOfThePowerSupplyId())) {
            errorMessages.add("Request For Cancellation id is not editable on executed object;");
        }
        editFiles(request, cancellation, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        archiveFiles(cancellation);
    }

    /**
     * Processes updates for cancellation requests in DRAFT status.
     *
     * @param cancellation  The cancellation entity to update
     * @param errorMessages List to collect validation error messages
     * @param request       The edit request containing changes
     * @throws ClientException if validation fails or referenced entities not found
     */
    public void editForDraft(
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            CancellationOfDisconnectionOfThePowerSupplyEditRequest request,
            List<String> errorMessages
    ) {
        checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_DRAFT);
        if (!request.getRequestForDisconnectionOfThePowerSupplyId().equals(cancellation.getRequestForDisconnectionOfThePowerSupplyId())) {
            if (!disconnectionPowerSupplyRequestRepository.existsByIdAndStatus(request.getRequestForDisconnectionOfThePowerSupplyId(), EntityStatus.ACTIVE)) {
                errorMessages.add("requestForDisconnectionOfThePowerSupplyId-[requestForDisconnectionOfThePowerSupplyId] not found;");
            } else {
                cancellation.setRequestForDisconnectionOfThePowerSupplyId(request.getRequestForDisconnectionOfThePowerSupplyId());
            }
        }
        if (!request.getRequestForDisconnectionOfThePowerSupplyId().equals(cancellation.getRequestForDisconnectionOfThePowerSupplyId()) && !request.getExistingPodChangeRequest().isEmpty()) {
            errorMessages.add("Editing old pods is not possible when selecting new request for disconnection;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.getRequestForDisconnectionOfThePowerSupplyId().equals(cancellation.getRequestForDisconnectionOfThePowerSupplyId())) {
            validateTableForEdit(request, cancellation, errorMessages);
        } else {
            validateTable(request, cancellation, errorMessages);
        }
    }

    /**
     * Updates files associated with a cancellation request.
     *
     * @param request       The edit request containing file changes
     * @param errorMessages List to collect validation error messages
     * @param cancellation  The cancellation entity to update files for
     */
    public void editFiles(
            CancellationOfDisconnectionOfThePowerSupplyEditRequest request,
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            List<String> errorMessages
    ) {
        Map<Long, PowerSupplyDcnCancellationFiles> fileMap = EPBListUtils.transformToMap(
                powerSupplyDcnCancellationFileRepository.findByIdsAndStatuses(request.getFileIds(), List.of(EntityStatus.ACTIVE)),
                PowerSupplyDcnCancellationFiles::getId
        );

        Set<PowerSupplyDcnCancellationFiles> existing = powerSupplyDcnCancellationFileRepository.findByCancellationIdAndSubObjectStatus(
                cancellation.getId(),
                EntityStatus.ACTIVE
        );

        for (Long fileId : request.getFileIds()) {
            PowerSupplyDcnCancellationFiles powerSupplyDcnCancellationFiles = fileMap.get(fileId);
            if (powerSupplyDcnCancellationFiles == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                powerSupplyDcnCancellationFiles.setPowerSupplyDcnCancellationId(cancellation.getId());
            }
        }

        for (PowerSupplyDcnCancellationFiles existingFile : existing) {
            PowerSupplyDcnCancellationFiles cancellationFiles = fileMap.get(existingFile.getId());
            if (cancellationFiles == null) {
                existingFile.setStatus(EntityStatus.DELETED);
            }
        }
    }

    /**
     * Validates POD table data during edit operations.
     *
     * @param request       The edit request containing POD changes
     * @param errorMessages List to collect validation error messages
     * @param cancellation  The cancellation entity being edited
     * @throws ClientException if POD data validation fails
     */
    public void validateTableForEdit(
            CancellationOfDisconnectionOfThePowerSupplyEditRequest request,
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            List<String> errorMessages
    ) {
        Set<CancellationPodQueryResponse> filterOldPods = cancellationOfDisconnectionOfThePowerSupplyRepository
                .findForDraft(request.getRequestForDisconnectionOfThePowerSupplyId(), cancellation.getId())
                .stream().filter(x -> x.getCancellationPodId() != null).map(CancellationPodQueryResponse::new).collect(Collectors.toSet());

        Set<CancellationPodQueryResponse> checkedTable = filterOldPods.stream().filter(CancellationPodQueryResponse::getChecked).collect(Collectors.toSet());

        for (CancellationPodRequest podRequest : request.getTable()) {
            if (podRequest.getCancellationReasonId() == null) {
                errorMessages.add("cancellation reason is mandatory!;");
            }
            if (!filterOldPods.contains(podRequest)) {
                errorMessages.add("Invalid input" + podRequest + ";");
            }
        }

        for (CancellationPodQueryResponse queryResponse : checkedTable) {
            if (!request.getTable().contains(queryResponse)) {
                errorMessages.add(queryResponse + " can't be unchecked");
            }
        }

        List<PowerSupplyDcnCancellationPods> cancellationDetailedToSave = request
                .getTable()
                .stream()
                .map(podRequest ->
                        powerSupplyDcnCancellationPodsRepository
                                .findFirstByPodIdAndPowerSupplyDcnCancellationId(podRequest.getPodId(), cancellation.getId())
                                .orElse(new PowerSupplyDcnCancellationPods(podRequest, cancellation.getId())))
                .toList();

        powerSupplyDcnCancellationPodsRepository.saveAll(cancellationDetailedToSave);

        Set<Long> cancellationReasonIds = reasonForCancellationRepository.findByIdsIn(
                request.getExistingPodChangeRequest()
                        .stream()
                        .map(CancellationPodChangeRequest::getCancellationReasonId)
                        .collect(Collectors.toSet()),
                List.of(NomenclatureItemStatus.ACTIVE)
        );

        Map<Long, PowerSupplyDcnCancellationPods> podMap = powerSupplyDcnCancellationPodsRepository.findByIdsIn(
                        request.getExistingPodChangeRequest()
                                .stream()
                                .map(CancellationPodChangeRequest::getCancellationPodId)
                                .collect(Collectors.toSet()),
                        cancellation.getId()
                )
                .stream()
                .collect(Collectors.toMap(
                                PowerSupplyDcnCancellationPods::getId,
                                Function.identity()
                        )
                );

        for (CancellationPodChangeRequest podChangeRequest : request.getExistingPodChangeRequest()) {
            PowerSupplyDcnCancellationPods cancellationOfPowerSupplyPods = podMap.get(podChangeRequest.getCancellationPodId());
            if (cancellationOfPowerSupplyPods == null) {
                errorMessages.add("reconnection pod with id " + podChangeRequest.getCancellationPodId() + " not found!;");
                continue;
            }
            if (!(podChangeRequest.getCancellationReasonId() != null && podChangeRequest.getCancellationReasonId().equals(cancellationOfPowerSupplyPods.getCancellationReasonId()))) {
                if (!cancellationReasonIds.contains(podChangeRequest.getCancellationReasonId())) {
                    errorMessages.add("cancellation reason not found with id" + podChangeRequest.getCancellationReasonId());
                } else {
                    cancellationOfPowerSupplyPods.setCancellationReasonId(podChangeRequest.getCancellationReasonId());
                }
            }
        }

        Set<Long> requestCancellationIds = EPBListUtils.transform(request.getTable(), CancellationPodRequest::getCancellationReasonId, Collectors.toSet());
        Set<Long> fetched = reasonForCancellationRepository.findByIdsIn(requestCancellationIds, List.of(NomenclatureItemStatus.ACTIVE));
        for (Long i : requestCancellationIds) {
            if (!fetched.contains(i)) {
                errorMessages.add("cancellation reason not found with id" + i + ";");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    /**
     * Checks if user has a specific permission.
     *
     * @param permission The permission to check
     * @throws ClientException if user lacks the permission
     */
    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY).contains(permission.getId()))
            throw new ClientException("You don't have appropriate permission: %s;".formatted(permission.name()), ErrorCode.OPERATION_NOT_ALLOWED);
    }

    /**
     * Retrieves a paginated view of the cancellation request table.
     *
     * @param id   The ID of the cancellation request
     * @param page Page number for pagination
     * @param size Number of records per page
     * @return Paginated list of PowerSupplyDcnCancellationTableResponse
     */
    public Page<PowerSupplyDcnCancellationTableResponse> table(Long id, int page, int size) {
        return cancellationOfDisconnectionOfThePowerSupplyRepository
                .findTableByRequestOfDisconnection(id, PageRequest.of(page, size))
                .map(PowerSupplyDcnCancellationTableResponse::new);
    }

    /**
     * Retrieves a filtered and paginated list of cancellation requests.
     *
     * @param request The listing request containing filter criteria
     * @return Paginated list of CancellationOfDisconnectionOfThePowerSupplyListingResponse
     */
    public Page<CancellationOfDisconnectionOfThePowerSupplyListingResponse> listing(CancellationOfDisconnectionOfThePowerSupplyListingRequest request) {
        LocalDateTime createDateFrom = null;
        LocalDateTime createDateTo = null;

        if (request.createDateFrom() != null) {
            createDateFrom = toLocalDateTimeToMidnight(request.createDateFrom());
        }
        if (request.createDateTo() != null) {
            createDateTo = toLocalDateTimeToUntilMidnight(request.createDateTo());
        }

        if (request.numberOfPodsFrom() != null && request.numberOfPodsTo() != null && request.numberOfPodsTo().equals(0) && request.numberOfPodsFrom().equals(0)) {
            return Page.empty();
        }

        List<CancellationOfDisconnectionOfThePowerSupplyStatus> cancellationStatuses = new ArrayList<>();
        List<CancellationOfDisconnectionOfThePowerSupplyStatus> requestStatuses = new ArrayList<>();
        if (request.cancellationStatus() != null) {
            requestStatuses.addAll(request.cancellationStatus());
        }
        if (hasViewDraftPermission() && (CollectionUtils.isEmpty(requestStatuses) || requestStatuses.contains(CancellationOfDisconnectionOfThePowerSupplyStatus.DRAFT))) {
            cancellationStatuses.add(CancellationOfDisconnectionOfThePowerSupplyStatus.DRAFT);
        }
        if (hasViewExecutedPermission() && (CollectionUtils.isEmpty(requestStatuses) || requestStatuses.contains(CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED))) {
            cancellationStatuses.add(CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED);
        }

        return cancellationOfDisconnectionOfThePowerSupplyRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                        ListUtils.emptyIfNull(request.gridOperatorId()),
                        request.numberOfPodsFrom(),
                        request.numberOfPodsTo(),
                        createDateFrom,
                        createDateTo,
                        EPBListingUtils.extractSearchBy(
                                request.searchBy(),
                                CancellationOfDisconnectionOfThePowerSupplyListingRequest.SearchBy.ALL
                        ),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(cancellationStatuses),
                        CollectionUtils.isEmpty(request.statuses()) ? List.of(EntityStatus.ACTIVE.name(), EntityStatus.DELETED.name()) : request.statuses().stream().map(Enum::name).toList(),
                        PageRequest.of(
                                request.page(),
                                request.size(),
                                EPBListingUtils.extractSortBy(
                                        request.sortDirection(),
                                        request.sortBy(),
                                        CancellationOfDisconnectionOfThePowerSupplyListingRequest.SortBy.CANCELLATION_NUMBER,
                                        CancellationOfDisconnectionOfThePowerSupplyListingRequest.SortBy::getColumnName
                                )
                        )
                )
                .map(CancellationOfDisconnectionOfThePowerSupplyListingResponse::new);
    }

    /**
     * Retrieves a cancellation request by ID.
     *
     * @param id The ID of the cancellation request
     * @return The found cancellation entity
     * @throws DomainEntityNotFoundException if not found
     */
    private CancellationOfDisconnectionOfThePowerSupply getCancellationOfDisconnectionOfThePowerSupply(Long id) {
        return cancellationOfDisconnectionOfThePowerSupplyRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Cancellation of disconnection of the power supply not found by ID %s {}".formatted(id))
                );
    }

    /**
     * Validates edit permissions based on request status.
     *
     * @param entityStatus Current entity status
     * @param status       Current cancellation status
     * @param id           Request ID
     * @throws AccessDeniedException        if user lacks required permissions
     * @throws OperationNotAllowedException if operation not allowed for current status
     */
    private void checkOnEditPermissionByStatuses(EntityStatus entityStatus, CancellationOfDisconnectionOfThePowerSupplyStatus status, Long id) {
        switch (entityStatus) {
            case ACTIVE -> {
                switch (status) {
                    case DRAFT -> {
                        if (!hasEditDraftPermission()) {
                            log.error("You do not have permission to edit draft cancellation of disconnection of the power supply.");
                            throw new AccessDeniedException("You do not have permission to edit draft cancellation of disconnection of the power supply.");
                        }
                    }
                    case EXECUTED -> {
                        if (!hasEditExecutedPermission()) {
                            log.error("You do not have permission to edit executed cancellation of disconnection of the power supply.");
                            throw new AccessDeniedException("You do not have permission to edit executed cancellation of disconnection of the power supply.");
                        }
                    }
                }
            }

            case DELETED -> {
                log.error("Cancellation of disconnection of the power supply with id {} is already deleted", id);
                throw new OperationNotAllowedException("Cancellation of disconnection of the power supply with id {} is already deleted;");
            }
        }
    }

    /**
     * Validates delete permissions based on request status.
     *
     * @param entityStatus Current entity status
     * @param status       Current cancellation status
     * @param id           Request ID
     * @throws OperationNotAllowedException if deletion not allowed
     */
    private void checkOnDeletePermissionByStatuses(EntityStatus entityStatus, CancellationOfDisconnectionOfThePowerSupplyStatus status, Long id) {
        switch (entityStatus) {
            case ACTIVE -> {
                if (Objects.requireNonNull(status) == CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED) {
                    log.error("Can't delete cancellation of disconnection of the power supply with status executed");
                    throw new OperationNotAllowedException("Can't delete cancellation of disconnection of the power supply with status executed");
                }
            }

            case DELETED -> {
                log.error("Cancellation of disconnection of the power supply with id {} is already deleted", id);
                throw new OperationNotAllowedException("Cancellation of disconnection of the power supply with id {} is already deleted;");
            }
        }
    }

    private boolean hasEditDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_DRAFT));
    }

    private boolean hasEditExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_EXECUTED));
    }

    private boolean hasViewDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DRAFT));
    }

    private boolean hasViewExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_EXECUTED));
    }

    private boolean checkOnPermission(List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY, requiredPermissions);
    }

    /**
     * Uploads and associates files with the cancellation request.
     *
     * @param file     The MultipartFile to upload
     * @param statuses List of document statuses to apply
     * @return FileWithStatusesResponse containing uploaded file details
     * @throws ClientException if file upload fails
     */
    public FileWithStatusesResponse uploadProxyFiles(MultipartFile file, List<DocumentFileStatus> statuses) {

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }

        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "proxy_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        PowerSupplyDcnCancellationFiles psdcFile = PowerSupplyDcnCancellationFiles
                .builder()
                .name(formattedFileName)
                .localFileUrl(url)
                .fileStatuses(statuses)
                .status(EntityStatus.ACTIVE)
                .build();

        var savedProxyFile = powerSupplyDcnCancellationFileRepository.saveAndFlush(psdcFile);
        return new FileWithStatusesResponse(savedProxyFile, accountManagerRepository.findByUserName(savedProxyFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Downloads a file associated with a cancellation request.
     *
     * @param id ID of the file to download
     * @return FileContent containing the file data
     * @throws DomainEntityNotFoundException if file not found
     */
    public FileContent downloadProxyFile(Long id) {
        var proxyFile = powerSupplyDcnCancellationFileRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getLocalFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }

    public FileContent downloadGeneratedDocument(Long id) {
        Document document = documentsRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("document with id %s not found".formatted(id)));

        var content = fileService.downloadFile(document.getSignedFileUrl());
        return new FileContent(document.getName(), content.getByteArray());
    }

    /**
     * Downloads a file, checking first if it needs to be retrieved from archives.
     *
     * @param id ID of the file to download
     * @return FileContent containing the file data
     * @throws Exception if file retrieval fails
     */
    public FileContent checkForArchivationAndDownload(Long id) throws Exception {
        var cancellationFile = powerSupplyDcnCancellationFileRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(cancellationFile.getIsArchived())) {
            if (Objects.isNull(cancellationFile.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(cancellationFile.getDocumentId(), cancellationFile.getFileId());

                return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
            }
        }

        var content = fileService.downloadFile(cancellationFile.getLocalFileUrl());
        return new FileContent(cancellationFile.getName(), content.getByteArray());
    }

    /**
     * Retrieves a paginated view of the POD table for a cancellation request.
     *
     * @param cancellationOfRequestId ID of the cancellation request
     * @param page                    Page number for pagination
     * @param size                    Number of records per page
     * @return Paginated list of ViewTableResponse
     */
    public Page<ViewTableResponse> tableView(Long cancellationOfRequestId, int page, int size) {
        CancellationOfDisconnectionOfThePowerSupply cancellation = getCancellationOfDisconnectionOfThePowerSupply(cancellationOfRequestId);

        if (cancellation.getEntityStatus().equals(EntityStatus.DELETED)) {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DELETED);
        } else {
            if (cancellation.getCancellationStatus().equals(CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED)) {
                checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_EXECUTED);
            } else {
                checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DRAFT);
            }

        }
        return cancellation.getCancellationStatus().equals(CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED) ?
                cancellationOfDisconnectionOfThePowerSupplyRepository.findForExecuted(cancellationOfRequestId, PageRequest.of(page, size)).map(ViewTableResponse::new)
                : cancellationOfDisconnectionOfThePowerSupplyRepository.findForDraftPage(cancellationOfRequestId, cancellation.getRequestForDisconnectionOfThePowerSupplyId(), PageRequest.of(page, size)).map(ViewTableResponse::new);
    }

    /**
     * Retrieves detailed view of a cancellation request.
     *
     * @param id ID of the cancellation request
     * @return CancellationView containing all cancellation request details
     * @throws DomainEntityNotFoundException if not found
     */
    public CancellationView view(Long id) {
        CancellationOfDisconnectionOfThePowerSupply cancellation = getCancellationOfDisconnectionOfThePowerSupply(id);

        if (cancellation.getEntityStatus().equals(EntityStatus.DELETED)) {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DELETED);
        }
        if (cancellation.getCancellationStatus().equals(CancellationOfDisconnectionOfThePowerSupplyStatus.EXECUTED)) {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_EXECUTED);
        } else {
            checkPermission(PermissionEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DRAFT);
        }

        DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestRepository.findById(cancellation.getRequestForDisconnectionOfThePowerSupplyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("RequestForDisconnection not found;"));

        CancellationView cancellationView = new CancellationView();
        setCancellationFiles(cancellation, cancellationView);

        cancellationView.setId(cancellation.getId());
        cancellationView.setCancellationStatus(cancellation.getCancellationStatus());
        cancellationView.setTasks(getTasks(cancellation.getId()));
        cancellationView.setEntityStatus(cancellation.getEntityStatus());
        cancellationView.setCreationDateAndTime(cancellation.getCreateDate());
        cancellationView.setRequestForDisconnectionId(disconnectionPowerSupplyRequests.getId());
        cancellationView.setRequestForDisconnectionNumber(disconnectionPowerSupplyRequests.getRequestNumber());
        cancellationView.setCancellationNumber(cancellation.getNumber());
        cancellationView.setTemplateResponses(findTemplatesForContract(cancellation.getId()));
        return cancellationView;
    }

    /**
     * Sets both uploaded and generated files for a cancellation request in the response.
     * Retrieves active files from PowerSupplyDcnCancellationFiles and Documents repositories,
     * maps them to CancellationFileResponse with manager display names.
     *
     * @param cancellation The cancellation entity containing the ID for file lookup
     * @param response     The view object where the files will be set
     */
    private void setCancellationFiles(CancellationOfDisconnectionOfThePowerSupply cancellation, CancellationView response) {
        List<CancellationFileResponse> files = new ArrayList<>(powerSupplyDcnCancellationFileRepository
                .findByCancellationIdAndSubObjectStatus(cancellation.getId(), EntityStatus.ACTIVE)
                .stream()
                .map(file -> new CancellationFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
                .toList());

        files.addAll(documentsRepository.findDocumentsForCancellation(cancellation.getId())
                .stream()
                .map(file -> new CancellationFileResponse(file, getManagerDisplayName(file.getSystemUserId())))
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

    /**
     * Converts LocalDate to LocalDateTime at midnight.
     *
     * @param localDate Date to convert
     * @return LocalDateTime at midnight of the given date
     */
    private LocalDateTime toLocalDateTimeToMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.MIDNIGHT;
        return LocalDateTime.of(localDate, time);
    }

    /**
     * Converts LocalDate to LocalDateTime at end of day.
     *
     * @param localDate Date to convert
     * @return LocalDateTime at 23:59:59.999999 of the given date
     */
    private LocalDateTime toLocalDateTimeToUntilMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.of(23, 59, 59, 999999);
        return LocalDateTime.of(localDate, time);
    }

    /**
     * Saves templates associated with a cancellation request.
     *
     * @param templateIds     Set of template IDs to save
     * @param productDetailId ID of the cancellation request
     * @param errorMessages   List to collect validation error messages
     */
    public void saveTemplates(Set<Long> templateIds, Long productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templateIds, ContractTemplatePurposes.CANCEL_DISCONNECTION_POWER, ContractTemplateStatus.ACTIVE);

        List<CancellationDcnTemplate> templateSubObjects = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            templateSubObjects.add(new CancellationDcnTemplate(templateId, productDetailId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        requestTemplatesRepository.saveAll(templateSubObjects);
    }

    /**
     * Updates templates associated with a cancellation request.
     *
     * @param templateIds     Set of template IDs to update
     * @param productDetailId ID of the cancellation request
     * @param errorMessages   List to collect validation error messages
     */
    public void updateTemplates(Set<Long> templateIds, Long productDetailId, List<String> errorMessages) {
        Map<Long, CancellationDcnTemplate> templateMap = requestTemplatesRepository.findByProductDetailId(productDetailId).stream().collect(Collectors.toMap(CancellationDcnTemplate::getTemplateId, j -> j));
        List<CancellationDcnTemplate> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            CancellationDcnTemplate remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new CancellationDcnTemplate(templateId, productDetailId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.CANCEL_DISCONNECTION_POWER, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<CancellationDcnTemplate> values = templateMap.values();
        for (CancellationDcnTemplate value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        requestTemplatesRepository.saveAll(templatesToSave);

    }

    /**
     * Retrieves templates associated with a cancellation request.
     *
     * @param productDetailId ID of the cancellation request
     * @return List of ContractTemplateShortResponse
     */
    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return requestTemplatesRepository.findForContract(productDetailId, LocalDate.now());
    }

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByCancellationId(id);
    }
}