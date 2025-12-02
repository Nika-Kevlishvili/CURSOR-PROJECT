package bg.energo.phoenix.service.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.*;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.customerReceivable.CustomerReceivableSearchBy;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.TableSearchBy;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.*;
import bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing.ReconnectionPowerSupplyListingListColumns;
import bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing.ReconnectionPowerSupplyListingRequest;
import bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing.ReconnectionPowerSupplyListingSearchByEnums;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.ReasonForCancellationRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.*;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ReconnectionOfPowerSupplyDocumentCreationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBListingUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.RECONNECTION_OF_POWER_SUPPLY;

/**
 * Service class for managing operations related to reconnection of power supply.
 * This service provides functionality for creating, updating, deleting, and querying
 * reconnection records, as well as handling associated files and templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconnectionOfThePowerSupplyService {
    private final static String RECONNECTION_PREFIX = "Reconnection-";
    private final ReconnectionOfThePowerSupplyRepository reconnectionOfThePowerSupplyRepository;
    private final PermissionService permissionService;
    private final GridOperatorRepository gridOperatorRepository;
    private final ReconnectionOfThePowerSupplyFilesRepository reconnectionOfThePowerSupplyFilesRepository;
    private final FileService fileService;
    private final ReasonForCancellationRepository reasonForCancellationRepository;
    private final ReconnectionOfThePowerSupplyDetailedInfoRepository reconnectionOfThePowerSupplyDetailedInfoRepository;
    private final TaskRepository taskRepository;
    private final ReconnectionOfThePowerSupplyExecutedFileRepository reconnectionOfThePowerSupplyExecutedFileRepository;
    private final AccountManagerRepository accountManagerRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private final ReconnectionOfPSTemplatesRepository powerSupplyTemplateRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;
    private final ReconnectionOfPowerSupplyDocumentCreationService reconnectionOfPowerSupplyDocumentCreationService;
    private final DocumentsRepository documentsRepository;
    private final TaskService taskService;

    /**
     * Creates a new reconnection of power supply entry.
     *
     * @param request The request object containing all necessary details for creating the reconnection.
     *                This includes grid operator information, file IDs, table data, and template IDs.
     * @return The ID of the newly created reconnection
     * @throws ClientException               if the save mode is invalid, required permissions are missing,
     *                                       or other validation errors occur during the creation process
     * @throws DomainEntityNotFoundException if referenced entities (e.g., grid operator) are not found
     */

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByReconnectionId(id);
    }
    @Transactional
    public Long create(ReconnectionOfThePowerSupplyBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (request.getSaveAs().equals(ReconnectionStatus.SAVE)) {
            throw new ClientException("Save is not valid on creation mode!;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        validatePermissions(request);
        validateExecutionForCreate(request, errorMessages);
        ReconnectionOfThePowerSupply reconnectionOfThePowerSupply = new ReconnectionOfThePowerSupply();
        checkAndSetGridOperator(request, errorMessages, reconnectionOfThePowerSupply);
        setRestFields(request, reconnectionOfThePowerSupply);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        reconnectionOfThePowerSupply = reconnectionOfThePowerSupplyRepository.saveAndFlush(reconnectionOfThePowerSupply);

        validateFiles(request, errorMessages, reconnectionOfThePowerSupply);
        validateTable(request, errorMessages, reconnectionOfThePowerSupply);
        reconnectionOfThePowerSupply.setReconnectionNumber(RECONNECTION_PREFIX + reconnectionOfThePowerSupply.getId());
        saveTemplates(request.getTemplateIds(), reconnectionOfThePowerSupply.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        if (request.getSaveAs().equals(ReconnectionStatus.EXECUTED)) {
            updatePods(reconnectionOfThePowerSupply.getId());
            reconnectionOfPowerSupplyDocumentCreationService.generateDocuments(reconnectionOfThePowerSupply.getId());
        }

        archiveFiles(reconnectionOfThePowerSupply);

        return reconnectionOfThePowerSupply.getId();
    }

    private void validateExecutionForCreate(ReconnectionOfThePowerSupplyBaseRequest request, List<String> errorMessages) {
        if (request.getSaveAs().equals(ReconnectionStatus.EXECUTED) && request.getTable().isEmpty()) {
            errorMessages.add("Pod data is required for EXECUTED status;");
        }
    }

    /**
     * Updates an existing reconnection of power supply entry.
     * <p>
     * This method handles updates differently based on the current status of the reconnection:
     * - For DRAFT status: Allows comprehensive updates including grid operator changes and table data modifications
     * - For EXECUTED status: Allows limited updates, primarily file-related changes
     *
     * @param request        The request object containing the updated details for the reconnection
     * @param reconnectionId The ID of the reconnection to update
     * @return The ID of the updated reconnection
     * @throws DomainEntityNotFoundException if the reconnection or associated entities are not found
     * @throws ClientException               if validation errors occur or the user lacks necessary permissions
     * @throws OperationNotAllowedException  if attempting invalid operations based on the current status
     */
    @Transactional
    public Long update(ReconnectionOfThePowerSupplyEditRequest request, Long reconnectionId) {
        List<String> errorMessages = new ArrayList<>();
        ReconnectionOfThePowerSupply reconnectionOfThePowerSupply = reconnectionOfThePowerSupplyRepository.findByIdAndStatusesIn(reconnectionId, List.of(EntityStatus.ACTIVE), List.of(ReconnectionStatus.DRAFT, ReconnectionStatus.EXECUTED))
                .orElseThrow(() -> new DomainEntityNotFoundException("Reconnection with such id not found!"));
        ReconnectionStatus reconnectionStatus = reconnectionOfThePowerSupply.getReconnectionStatus();
        switch (reconnectionStatus) {
            case EXECUTED -> editForExecuted(reconnectionOfThePowerSupply, errorMessages, request);
            case DRAFT -> {
                editForDraft(reconnectionOfThePowerSupply, errorMessages, request);
                reconnectionOfThePowerSupply.setReconnectionStatus(request.getSaveAs());
            }
        }
        editFiles(reconnectionOfThePowerSupply, errorMessages, request);
        updateTemplates(request.getTemplateIds(), reconnectionOfThePowerSupply.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        if (request.getSaveAs().equals(ReconnectionStatus.EXECUTED)) {
            updatePods(reconnectionId);
            reconnectionOfPowerSupplyDocumentCreationService.generateDocuments(reconnectionOfThePowerSupply.getId());
        }

        archiveFiles(reconnectionOfThePowerSupply);

        return reconnectionOfThePowerSupply.getId();
    }

    /**
     * Retrieves a paginated table view of reconnections based on the provided request parameters.
     * <p>
     * This method returns a page of {@link TableViewResponse} objects containing summarized information about reconnections.
     * The view is tailored based on the reconnection's status (DRAFT or EXECUTED) and the user's permissions.
     *
     * @param request The request object containing parameters for filtering and pagination
     * @param id      The ID of the reconnection to retrieve the table view for
     * @return A page of {@link TableViewResponse} objects representing the table view of the reconnection
     * @throws DomainEntityNotFoundException if the reconnection is not found
     * @throws ClientException               if the user doesn't have appropriate permissions to view the reconnection
     */
    public Page<TableViewResponse> tableView(ReconnectionTablePreviewRequest request, Long id) {
        ReconnectionOfThePowerSupply reconnectionOfThePowerSupply = reconnectionOfThePowerSupplyRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reconnection with such id not found!"));
        if (reconnectionOfThePowerSupply.getStatus().equals(EntityStatus.DELETED)) {
            checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DELETED);
        } else {
            if (reconnectionOfThePowerSupply.getReconnectionStatus().equals(ReconnectionStatus.EXECUTED)) {
                checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED);
            } else {
                checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT);
            }

        }
        return reconnectionOfThePowerSupply.getReconnectionStatus().equals(ReconnectionStatus.EXECUTED) ?
                reconnectionOfThePowerSupplyRepository.findForExecuted(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt())
                        , getSearchByEnum(request.getSearchBy()), reconnectionOfThePowerSupply.getId(), PageRequest.of(request.getPage(), request.getPageSize())).map(TableViewResponse::new)
                : reconnectionOfThePowerSupplyRepository.findForDraftPage(reconnectionOfThePowerSupply.getGridOperatorId(), EPBStringUtils.fromPromptToQueryParameter(request.getPrompt())
                , getSearchByEnum(request.getSearchBy()), reconnectionOfThePowerSupply.getId(), PageRequest.of(request.getPage(), request.getPageSize())).map(TableViewResponse::new);
    }

    /**
     * Retrieves a detailed view of a specific reconnection.
     * <p>
     * This method compiles comprehensive information about a reconnection, including:
     * - Basic reconnection details (status, creation date, etc.)
     * - Associated grid operator information
     * - List of associated files with their statuses
     * - Active tasks related to the reconnection
     * - Template information
     * <p>
     * The view is tailored based on the reconnection's status (DRAFT, EXECUTED, or DELETED)
     * and the user's permissions.
     *
     * @param id The ID of the reconnection to view
     * @return A ReconnectionView object containing detailed information about the reconnection
     * @throws DomainEntityNotFoundException if the reconnection is not found
     * @throws ClientException               if the user doesn't have appropriate permissions to view the reconnection
     */
    public ReconnectionView view(Long id) {
        ReconnectionOfThePowerSupply reconnectionOfThePowerSupply = reconnectionOfThePowerSupplyRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reconnection with such id not found!"));
        if (reconnectionOfThePowerSupply.getStatus().equals(EntityStatus.DELETED)) {
            checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DELETED);
        } else {
            if (reconnectionOfThePowerSupply.getReconnectionStatus().equals(ReconnectionStatus.EXECUTED)) {
                checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED);
            } else {
                checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT);
            }

        }

        GridOperator gridOperator = gridOperatorRepository.findById(reconnectionOfThePowerSupply.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Grid operator not found!;"));
        ShortResponse shortResponse = new ShortResponse(gridOperator.getId(), gridOperator.getName());
        List<TaskShortResponse> reconnectionOfThePowerSupplyActiveTasks = taskRepository.findReconnectionOfThePowerSupplyActiveTasks(reconnectionOfThePowerSupply.getId());

        ReconnectionView reconnectionView = new ReconnectionView();
        reconnectionView.setReconnectionStatus(reconnectionOfThePowerSupply.getReconnectionStatus());
        reconnectionView.setTasks(reconnectionOfThePowerSupplyActiveTasks);
        reconnectionView.setGeneralStatus(reconnectionOfThePowerSupply.getStatus());
        reconnectionView.setCreationDateAndTime(reconnectionOfThePowerSupply.getCreateDate());
        reconnectionView.setGridOperatorResponse(shortResponse);
        reconnectionView.setTemplateResponses(findTemplatesForContract(reconnectionOfThePowerSupply.getId()));
        setFiles(reconnectionOfThePowerSupply, reconnectionView);
        return reconnectionView;
    }

    private void setFiles(ReconnectionOfThePowerSupply reconnectionOfThePowerSupply, ReconnectionView response) {
        List<ReconnectionOfPowerSupplyFileResponse> reconnectionOfPowerSupplyFileResponses = new ArrayList<>();

        List<ReconnectionOfPowerSupplyFileResponse> reconnectionRequestsFiles =
                reconnectionOfThePowerSupplyFilesRepository
                        .findByReconnectionIdAndSubObjectStatus(
                                reconnectionOfThePowerSupply.getId(),
                                EntityStatus.ACTIVE)
                        .stream()
                        .map(file ->
                                new ReconnectionOfPowerSupplyFileResponse(
                                        file,
                                        accountManagerRepository.findByUserName(file.getSystemUserId())
                                                .map(manager -> " ("
                                                        .concat(manager.getDisplayName())
                                                        .concat(")")
                                                )
                                                .orElse("")
                                )
                        )
                        .toList();

        reconnectionOfPowerSupplyFileResponses.addAll(reconnectionRequestsFiles);

        List<ReconnectionOfPowerSupplyFileResponse> reconnectionRequestsDocuments =
                documentsRepository
                        .findDocumentsForReconnectionRequest(
                                reconnectionOfThePowerSupply.getId()
                        )
                        .stream()
                        .map(f ->
                                new ReconnectionOfPowerSupplyFileResponse(
                                        f,
                                        accountManagerRepository.findByUserName(f.getSystemUserId())
                                                .map(manager -> " ("
                                                        .concat(manager.getDisplayName())
                                                        .concat(")"))
                                                .orElse("")))
                        .toList();

        reconnectionOfPowerSupplyFileResponses.addAll(reconnectionRequestsDocuments);

        response.setFiles(reconnectionOfPowerSupplyFileResponses);
    }

    /**
     * Edits a reconnection of power supply in the executed state.
     * <p>
     * This method performs the following validations and updates:
     * - Checks if the user has the required permission to edit an executed reconnection.
     * - Ensures the request is to save the reconnection, and not to change the status.
     * - Verifies that the table cannot be edited in the executed mode.
     * - Checks if the grid operator ID cannot be changed for an executed reconnection.
     * - If a file is provided, downloads the file content, parses the content, and updates the reconnection details.
     *
     * @param reconnectionOfThePowerSupply The reconnection of power supply entity to be edited.
     * @param errorMessages                A list to store any error messages encountered during the edit process.
     * @param request                      The edit request containing the updated information.
     */
    public void editForExecuted(ReconnectionOfThePowerSupply reconnectionOfThePowerSupply, List<String> errorMessages, ReconnectionOfThePowerSupplyEditRequest request) {
        checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_EDIT_EXECUTED);
        if (!request.getSaveAs().equals(ReconnectionStatus.SAVE)) {
            errorMessages.add("It's not possible to save as execute or draft on executed object;");
        }
        if (!request.getTable().isEmpty()) {
            errorMessages.add("It's not possible to edit table on executed mode!;");
        }
        if (!request.getGridOperatorId().equals(reconnectionOfThePowerSupply.getGridOperatorId())) {
            errorMessages.add("Grid operator id is not editable on executed object;");
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.getFileId() != null) {
            FileContent fileContent = downloadProxyFileForExecuted(request.getFileId());
            List<PodDate> parsed = parseContent(fileContent.getContent());

            Set<String> collect = parsed.stream().map(PodDate::getPodIdentifier).collect(Collectors.toSet());
            Map<String, ReconnectionOfThePowerSupplyPods> podMap = reconnectionOfThePowerSupplyDetailedInfoRepository.findByPodIdentifiersAndReconnectionId(
                    collect, reconnectionOfThePowerSupply.getId()
            ).stream().collect(Collectors.toMap(x -> ((PointOfDelivery) x[0]).getIdentifier(), y -> (ReconnectionOfThePowerSupplyPods) y[1]));

            for (PodDate podDate : parsed) {
                ReconnectionOfThePowerSupplyPods reconnectionOfThePowerSupplyPods = podMap.get(podDate.getPodIdentifier());
                if (reconnectionOfThePowerSupplyPods == null) {
                    log.debug("Pod doesn't exist with identifier {}, skipping!;", podDate.getPodIdentifier());
                    continue;
                }
                reconnectionOfThePowerSupplyPods.setReconnectionDate(podDate.getReconnectionDate());
            }
        } else {
            updateReconnectionPods(request, errorMessages, reconnectionOfThePowerSupply);
        }
    }

    private void updateReconnectionPods(ReconnectionOfThePowerSupplyEditRequest request, List<String> errorMessages, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        Map<Long, ReconnectionOfThePowerSupplyPods> podMap = reconnectionOfThePowerSupplyDetailedInfoRepository.findByReconnectionId(reconnectionOfThePowerSupply.getId())
                .stream().collect(Collectors.toMap(ReconnectionOfThePowerSupplyPods::getId, j -> j));

        for (ReconnectionPodChangeRequest podChangeRequest : request.getExistingPodChangeRequest()) {
            ReconnectionOfThePowerSupplyPods reconnectionOfThePowerSupplyPods = podMap.get(podChangeRequest.getReconnectionPodId());
            if (reconnectionOfThePowerSupplyPods == null) {
                errorMessages.add("existing reconnection pod with id " + podChangeRequest.getReconnectionPodId() + " not found!;");
                continue;
            }
            if (podChangeRequest.getReconnectionDate() != null) {
                reconnectionOfThePowerSupplyPods.setReconnectionDate(podChangeRequest.getReconnectionDate());
            }
        }
        reconnectionOfThePowerSupplyDetailedInfoRepository.saveAll(podMap.values());
    }

    /**
     * Parses the content of an Excel file and extracts the POD identifier and reconnection date for each row.
     *
     * @param content The byte array containing the Excel file content.
     * @return A list of {@link PodDate} objects, each containing the POD identifier and reconnection date.
     * @throws ClientException if an error occurs during the parsing of the file content.
     */

    public List<PodDate> parseContent(byte[] content) {
        List<PodDate> podDates = new ArrayList<>();
        try (ByteArrayInputStream baIs = new ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(baIs)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                XSSFRow row = sheet.getRow(rowNum);
                if (row != null) {
                    String podIdentifier = EPBExcelUtils.getStringValue(0, row);
                    if (podIdentifier == null) {
                        continue;
                    }
                    LocalDate reconnectionDate = EPBExcelUtils.getLocalDateValue(1, row);
                    PodDate podDate = new PodDate();
                    podDate.setPodIdentifier(podIdentifier);
                    podDate.setReconnectionDate(reconnectionDate);
                    podDates.add(podDate);
                }
            }

        } catch (IOException e) {
            throw new ClientException("Error during parsing file content", ErrorCode.APPLICATION_ERROR);
        }
        return podDates;
    }


    /**
     * Edits the draft of a reconnection of power supply.
     * <p>
     * This method performs the following checks and operations:
     * 1. Checks if the user has the necessary permission to edit the draft.
     * 2. Checks if the request includes a file ID, which is not allowed in draft mode.
     * 3. Checks if the request is trying to save the draft, which is not valid.
     * 4. Checks if the grid operator ID in the request is different from the one in the reconnection, and if so, sets the new grid operator.
     * 5. Checks if the grid operator ID is different and the request includes existing POD change requests, which is not allowed.
     * 6. Throws an exception if any of the error messages are not empty.
     * 7. Validates the table for editing if the grid operator ID is the same, or validates the table if the grid operator ID is different.
     *
     * @param reconnectionOfThePowerSupply The reconnection of power supply object to be edited.
     * @param errorMessages                The list of error messages to be populated.
     * @param request                      The request object containing the details for editing the reconnection.
     */
    public void editForDraft(ReconnectionOfThePowerSupply reconnectionOfThePowerSupply, List<String> errorMessages, ReconnectionOfThePowerSupplyEditRequest request) {
        checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT);
        if (request.getFileId() != null) {
            errorMessages.add("File import is not allowed in draft mode;");
        }
        if (request.getSaveAs().equals(ReconnectionStatus.SAVE)) {
            errorMessages.add("Save is not valid on draft object;");
        }
        Long gridOperatorId = reconnectionOfThePowerSupply.getGridOperatorId();
        if (!request.getGridOperatorId().equals(reconnectionOfThePowerSupply.getGridOperatorId())) {
            checkAndSetGridOperator(request, errorMessages, reconnectionOfThePowerSupply);
        }
        if (!request.getGridOperatorId().equals(reconnectionOfThePowerSupply.getGridOperatorId()) && !request.getExistingPodChangeRequest().isEmpty()) {
            errorMessages.add("Editing old pods is not possible when selecting new grid operator;");
        }
        if (request.getTable().isEmpty() && request.getExistingPodChangeRequest().isEmpty() && request.getSaveAs().equals(ReconnectionStatus.EXECUTED)) {
            errorMessages.add("Pod data is required for EXECUTED status;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.getGridOperatorId().equals(gridOperatorId)) {
            validateTableForEdit(request, errorMessages, reconnectionOfThePowerSupply);
        } else {
            validateTable(request, errorMessages, reconnectionOfThePowerSupply);
        }

    }


    /**
     * Edits the files associated with a reconnection of power supply.
     * <p>
     * This method performs the following operations:
     * 1. Retrieves the files specified in the request, filtering by their IDs and active status.
     * 2. Finds the existing files associated with the reconnection.
     * 3. For each file ID in the request:
     * - If the file is found, sets the power supply reconnection ID on the file.
     * - If the file is not found, adds an error message to the errorMessages list.
     * 4. For each existing file associated with the reconnection:
     * - If the file is not found in the request, sets the status of the existing file to DELETED.
     *
     * @param reconnectionOfThePowerSupply The reconnection of power supply object.
     * @param errorMessages                The list of error messages to be populated.
     * @param request                      The request object containing the file IDs to be edited.
     */
    public void editFiles(ReconnectionOfThePowerSupply reconnectionOfThePowerSupply, List<String> errorMessages, ReconnectionOfThePowerSupplyEditRequest request) {
        Map<Long, ReconnectionOfThePowerSupplyFiles> fileMap = reconnectionOfThePowerSupplyFilesRepository.findByIdsAndStatuses(request.getFileIds(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ReconnectionOfThePowerSupplyFiles::getId, f -> f));
        Set<ReconnectionOfThePowerSupplyFiles> existing = reconnectionOfThePowerSupplyFilesRepository.findByReconnectionIdAndSubObjectStatus(reconnectionOfThePowerSupply.getId(), EntityStatus.ACTIVE);

        for (Long fileId : request.getFileIds()) {
            ReconnectionOfThePowerSupplyFiles reconnectionOfThePowerSupplyFiles = fileMap.get(fileId);
            if (reconnectionOfThePowerSupplyFiles == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                reconnectionOfThePowerSupplyFiles.setPowerSupplyReconnectionId(reconnectionOfThePowerSupply.getId());
            }
        }

        for (ReconnectionOfThePowerSupplyFiles existingFile : existing) {
            ReconnectionOfThePowerSupplyFiles reconnectionFile = fileMap.get(existingFile.getId());
            if (reconnectionFile == null) {
                existingFile.setStatus(EntityStatus.DELETED);
            }
        }
    }

    /**
     * Marks a reconnection of power supply entry as deleted.
     * <p>
     * This method performs a soft delete operation, changing the status of the reconnection to DELETED.
     * It includes the following checks and operations:
     * 1. Verifies that the reconnection exists and is not already deleted
     * 2. Ensures that the reconnection is not in EXECUTED status, as executed reconnections cannot be deleted
     * 3. Updates the status of the reconnection to DELETED
     *
     * @param id The ID of the reconnection to delete
     * @return The ID of the deleted reconnection
     * @throws ClientException               if the reconnection is already deleted or has EXECUTED status
     * @throws DomainEntityNotFoundException if the reconnection with the given ID is not found
     * @throws OperationNotAllowedException  if attempting to delete an EXECUTED reconnection
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting Reconnection of Power Supply with id: {}", id);

        ReconnectionOfThePowerSupply reconnectionOfThePowerSupply = reconnectionOfThePowerSupplyRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("Can't find Reconnection of Power Supply with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (reconnectionOfThePowerSupply.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Reconnection of Power Supply is already deleted;");
            throw new ClientException("Reconnection of Power Supply is already deleted;", ErrorCode.APPLICATION_ERROR);
        }

        if (Objects.equals(reconnectionOfThePowerSupply.getReconnectionStatus(), ReconnectionStatus.EXECUTED)) {
            log.error("can not delete Reconnection of Power Supply with EXECUTED status;");
            throw new OperationNotAllowedException("can not delete Reconnection of Power Supply with EXECUTED status;");
        }

        reconnectionOfThePowerSupply.setStatus(EntityStatus.DELETED);
        reconnectionOfThePowerSupplyRepository.save(reconnectionOfThePowerSupply);
        return reconnectionOfThePowerSupply.getId();
    }

    /**
     * Filters and retrieves a paginated list of reconnections based on various criteria.
     * This method supports advanced filtering and sorting capabilities, including:
     * - Text-based search across multiple fields
     * - Date range filtering
     * - Status-based filtering (DRAFT, EXECUTED, DELETED)
     * - Grid operator filtering
     * The results are permission-aware, only returning reconnections the user has access to view.
     *
     * @param request The request object containing all filter criteria, pagination, and sorting parameters
     * @return A Page of ReconnectionPowerSupplyListingResponse objects matching the filter criteria
     * @throws IllegalArgumentException if date ranges or POD number ranges in the request are invalid
     */
    public Page<ReconnectionPowerSupplyListingResponse> filter(ReconnectionPowerSupplyListingRequest request) {
        Sort order = EPBListingUtils.extractSortBy(request.getDirection(), request.getSortBy(), ReconnectionPowerSupplyListingListColumns.NUMBER, ReconnectionPowerSupplyListingListColumns::getValue);
        if (Objects.nonNull(request.getCreateDateFrom()) && Objects.nonNull(request.getCreateDateTo()) && request.getCreateDateFrom().isAfter(request.getCreateDateTo())) {
            throw new IllegalArgumentException("createDateFrom-createDateFrom can not be after createDateTo;");
        }
        if (Objects.nonNull(request.getNumberOfPodsFrom()) && Objects.nonNull(request.getNumberOfPodsTo()) && request.getNumberOfPodsFrom() > (request.getNumberOfPodsTo())) {
            throw new IllegalArgumentException("numberOfPodsFrom-numberOfPodsFrom can not be more than numberOfPodsTo;");
        }

        List<String> entityStatuses = getReconnectionEntityStatuses();
        if (!CollectionUtils.isEmpty(request.getStatuses())) {
            entityStatuses.retainAll(request.getStatuses().stream().map(Enum::name).collect(Collectors.toList()));
        }

        return reconnectionOfThePowerSupplyRepository.filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListingUtils.extractSearchBy(request.getSearchBy(), ReconnectionPowerSupplyListingSearchByEnums.ALL),
                        Objects.isNull(request.getPsdrDirection()) ? Sort.Direction.DESC.name() : request.getPsdrDirection().name(),
                        request.getCreateDateFrom(),
                        request.getCreateDateTo(),
                        getReconnectionModifiedStatuses(request.getReconnectionStatus()),
                        request.getNumberOfPodsFrom(),
                        request.getNumberOfPodsTo(),
                        entityStatuses,
                        CollectionUtils.isEmpty(request.getGridOperatorIds()) ? new ArrayList<>() : request.getGridOperatorIds(),
                        PageRequest.of(request.getPage(), request.getSize(), order))
                .map(ReconnectionPowerSupplyListingResponse::new);
    }

    /**
     * Validates the table of reconnection pods in the given request, ensuring that the requested changes are valid.
     * This method performs the following checks:
     * - Retrieves the existing reconnection pods for the given grid operator and filters them to only include checked pods.
     * - Checks that the cancellation reason is provided for each requested pod.
     * - Ensures that the requested pods are valid and exist in the checked table.
     * - Checks that any pods that were previously checked are still included in the request.
     * - Validates the cancellation reasons for any existing pods that are being updated.
     * - Ensures that all existing pods are included in the request.
     * - Saves the updated reconnection pod details to the database.
     *
     * @param request                      The request object containing the table of reconnection pods to be validated.
     * @param errorMessages                A list to store any error messages encountered during validation.
     * @param reconnectionOfThePowerSupply The reconnection of power supply entity being updated.
     */
    private void validateTable(ReconnectionOfThePowerSupplyBaseRequest request, List<String> errorMessages, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        Set<ReconnectionPodQueryResponse> table = reconnectionOfThePowerSupplyRepository
                .findForCheck(request.getGridOperatorId(), EPBStringUtils.fromPromptToQueryParameter(null), getSearchByEnum(null))
                .stream().map(ReconnectionPodQueryResponse::new).collect(Collectors.toSet());
        Set<ReconnectionPodQueryResponse> checkedTable = table.stream().filter(ReconnectionPodQueryResponse::getChecked).collect(Collectors.toSet());
        checkSubsetsAndCancellationIds(request, errorMessages, table, checkedTable);
        List<ReconnectionOfThePowerSupplyPods> reconnectionDetailedToSave = request.getTable()
                .stream().map(podRequest -> new ReconnectionOfThePowerSupplyPods(podRequest, reconnectionOfThePowerSupply.getId())).toList();
        reconnectionOfThePowerSupplyDetailedInfoRepository.saveAll(reconnectionDetailedToSave);
    }

    /**
     * Validates the table of reconnection pods in the given request, ensuring that the requested changes are valid.
     * This method performs the following checks:
     * - Retrieves the existing reconnection pods for the given grid operator and filters them to only include checked pods.
     * - Checks that the cancellation reason is provided for each requested pod.
     * - Ensures that the requested pods are valid and exist in the checked table.
     * - Checks that any pods that were previously checked are still included in the request.
     * - Validates the cancellation reasons for any existing pods that are being updated.
     * - Ensures that all existing pods are included in the request.
     * - Saves the updated reconnection pod details to the database.
     *
     * @param request                      The request object containing the table of reconnection pods to be validated.
     * @param errorMessages                A list to store any error messages encountered during validation.
     * @param reconnectionOfThePowerSupply The reconnection of power supply entity being updated.
     */
    private void validateTableForEdit(ReconnectionOfThePowerSupplyEditRequest request, List<String> errorMessages, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        Set<ReconnectionPodQueryResponse> table = reconnectionOfThePowerSupplyRepository
                .findForDraft(request.getGridOperatorId(), EPBStringUtils.fromPromptToQueryParameter(null), getSearchByEnum(null), reconnectionOfThePowerSupply.getId())
                .stream().map(ReconnectionPodQueryResponse::new).collect(Collectors.toSet());

        Set<Long> oldPodsWhichCantBeUnchecked = table.stream().filter(x -> x.getReconnectionPodId() != null && x.getUnableToUncheck()).
                map(ReconnectionPodQueryResponse::getReconnectionPodId).collect(Collectors.toSet());


        Set<ReconnectionPodQueryResponse> filterOldPods = table.stream().filter(x -> x.getReconnectionPodId() == null).collect(Collectors.toSet());
        Set<ReconnectionPodQueryResponse> checkedTable = filterOldPods.stream().filter(ReconnectionPodQueryResponse::getChecked).collect(Collectors.toSet());
        for (ReconnectionPodRequest podRequest : request.getTable()) {
            if (podRequest.getCancellationReasonId() == null) {
                errorMessages.add("cancellation reason is mandatory!;");
            }
            if (!filterOldPods.contains(podRequest)) {
                errorMessages.add("Invalid input" + podRequest + ";");
            }
        }
        for (ReconnectionPodQueryResponse queryResponse : checkedTable) {

            if (!request.getTable().contains(queryResponse)) {
                errorMessages.add(queryResponse + " can't be unchecked");
            }
        }
        Set<Long> cancellationReasonIds = reasonForCancellationRepository.findByIdsIn(request.getExistingPodChangeRequest().stream().map(ReconnectionPodChangeRequest::getCancellationReasonId).collect(Collectors.toSet()), List.of(NomenclatureItemStatus.ACTIVE));
        Map<Long, ReconnectionOfThePowerSupplyPods> podMap = reconnectionOfThePowerSupplyDetailedInfoRepository.findByReconnectionId(reconnectionOfThePowerSupply.getId())
                .stream().collect(Collectors.toMap(ReconnectionOfThePowerSupplyPods::getId, j -> j));

        for (ReconnectionPodChangeRequest podChangeRequest : request.getExistingPodChangeRequest()) {
            ReconnectionOfThePowerSupplyPods reconnectionOfThePowerSupplyPods = podMap.get(podChangeRequest.getReconnectionPodId());
            if (reconnectionOfThePowerSupplyPods == null) {
                errorMessages.add("existing reconnection pod with id " + podChangeRequest.getReconnectionPodId() + " not found!;");
                continue;
            }
            if (!(podChangeRequest.getCancellationReasonId() != null && podChangeRequest.getCancellationReasonId().equals(reconnectionOfThePowerSupplyPods.getCancellationReasonId()))) {
                if (!cancellationReasonIds.contains(podChangeRequest.getCancellationReasonId())) {
                    errorMessages.add("cancellation reason not found with id" + podChangeRequest.getCancellationReasonId());
                } else {
                    reconnectionOfThePowerSupplyPods.setCancellationReasonId(podChangeRequest.getCancellationReasonId());
                }
            }
            if (podChangeRequest.getReconnectionDate() != null) {
                reconnectionOfThePowerSupplyPods.setReconnectionDate(podChangeRequest.getReconnectionDate());
            }

        }
        Set<Long> requestPods = request.getExistingPodChangeRequest().stream().map(ReconnectionPodChangeRequest::getReconnectionPodId).collect(Collectors.toSet());
        List<Long> toRemove = new ArrayList<>();
        for (Long i : podMap.keySet()) {
            if (!requestPods.contains(i)) {
                if (oldPodsWhichCantBeUnchecked.contains(i)) {
                    errorMessages.add("reconnection pod is mandatory with id " + i + ";");
                } else {
                    toRemove.add(i);
                }
            }
        }
        reconnectionOfThePowerSupplyDetailedInfoRepository.deleteAllByIdInBatch(toRemove);
        Set<Long> requestCancellationIds = request.getTable().stream().map(ReconnectionPodRequest::getCancellationReasonId).collect(Collectors.toSet());
        Set<Long> fetched = reasonForCancellationRepository.findByIdsIn(requestCancellationIds, List.of(NomenclatureItemStatus.ACTIVE));
        for (Long i : requestCancellationIds) {
            if (!fetched.contains(i)) {
                errorMessages.add("cancellation reason not found with id" + i + ";");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        List<ReconnectionOfThePowerSupplyPods> reconnectionDetailedToSave = request.getTable()
                .stream().map(podRequest -> new ReconnectionOfThePowerSupplyPods(podRequest, reconnectionOfThePowerSupply.getId())).toList();
        reconnectionOfThePowerSupplyDetailedInfoRepository.saveAll(reconnectionDetailedToSave);
    }

    /**
     * Validates the subsets and cancellation IDs in the provided request.
     * <p>
     * This method performs the following checks:
     * 1. Ensures that all checked items in the `checkedTable` are present in the `request.getTable()`.
     * 2. Checks that each item in `request.getTable()` has a non-null `cancellationReasonId`.
     * 3. Checks that each item in `request.getTable()` is present in the `table` set.
     * 4. Fetches the active cancellation reasons for the IDs in `request.getTable()` and ensures they all exist.
     * 5. Throws an exception if any of the above checks fail, with the collected error messages.
     *
     * @param request       The request object containing the data to be validated.
     * @param errorMessages A list to collect error messages during the validation process.
     * @param table         A set of `ReconnectionPodQueryResponse` objects representing the valid input.
     * @param checkedTable  A set of `ReconnectionPodQueryResponse` objects representing the checked items.
     */
    private void checkSubsetsAndCancellationIds(ReconnectionOfThePowerSupplyBaseRequest request, List<String> errorMessages, Set<ReconnectionPodQueryResponse> table, Set<ReconnectionPodQueryResponse> checkedTable) {
        for (ReconnectionPodQueryResponse queryResponse : checkedTable) {

            if (!request.getTable().contains(queryResponse)) {
                errorMessages.add(queryResponse + " can't be unchecked");
            }
        }
        for (ReconnectionPodRequest podRequest : request.getTable()) {
            if (podRequest.getCancellationReasonId() == null) {
                errorMessages.add("cancellation reason is mandatory!;");
            }
            if (!table.contains(podRequest)) {
                errorMessages.add("Invalid input" + podRequest + ";");
            }
        }
        Set<Long> requestCancellationIds = request.getTable().stream().map(ReconnectionPodRequest::getCancellationReasonId).collect(Collectors.toSet());
        Set<Long> fetched = reasonForCancellationRepository.findByIdsIn(requestCancellationIds, List.of(NomenclatureItemStatus.ACTIVE));
        for (Long i : requestCancellationIds) {
            if (!fetched.contains(i)) {
                errorMessages.add("cancellation reason not found with id" + i + ";");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    private void validateFiles(ReconnectionOfThePowerSupplyBaseRequest request, List<String> errorMessages, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        Map<Long, ReconnectionOfThePowerSupplyFiles> fileMap = reconnectionOfThePowerSupplyFilesRepository.findByIdsAndStatuses(request.getFileIds(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ReconnectionOfThePowerSupplyFiles::getId, f -> f));
        for (Long fileId : request.getFileIds()) {
            ReconnectionOfThePowerSupplyFiles reconnectionOfThePowerSupplyFiles = fileMap.get(fileId);
            if (reconnectionOfThePowerSupplyFiles == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                reconnectionOfThePowerSupplyFiles.setPowerSupplyReconnectionId(reconnectionOfThePowerSupply.getId());
            }
        }
    }

    private void setRestFields(ReconnectionOfThePowerSupplyBaseRequest request, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        reconnectionOfThePowerSupply.setReconnectionNumber("TEMP");
        reconnectionOfThePowerSupply.setStatus(EntityStatus.ACTIVE);
        reconnectionOfThePowerSupply.setReconnectionStatus(request.getSaveAs());
    }

    private void checkAndSetGridOperator(ReconnectionOfThePowerSupplyBaseRequest request, List<String> errorMessages, ReconnectionOfThePowerSupply reconnectionOfThePowerSupply) {
        var gridOperatorOptional = gridOperatorRepository.findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (gridOperatorOptional.isEmpty()) {
            errorMessages.add("gridOperatorId-[gridOperatorId] grid operator not found!;");
        }
        reconnectionOfThePowerSupply.setGridOperatorId(request.getGridOperatorId());
    }

    private void validatePermissions(ReconnectionOfThePowerSupplyBaseRequest request) {
        if (request.getSaveAs().equals(ReconnectionStatus.DRAFT)) {
            checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_CREATE_DRAFT);
        } else {
            checkPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_CREATE_EXECUTE);
        }
    }


    /**
     * Checks if the current user has the specified permission for the "Reconnection of Power Supply" functionality.
     *
     * @param permission The permission to check for.
     * @throws ClientException if the user does not have the required permission.
     */
    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(RECONNECTION_OF_POWER_SUPPLY).contains(permission.getId()))
            throw new ClientException("You don't have appropriate permission: %s;".formatted(permission.name()), ErrorCode.OPERATION_NOT_ALLOWED);
    }


    /**
     * Uploads a file associated with a reconnection.
     * <p>
     * This method handles the file upload process, including:
     * 1. Sanitizing and formatting the file name
     * 2. Generating a unique file name to prevent conflicts
     * 3. Uploading the file to a specified FTP location
     * 4. Creating a database record for the uploaded file
     * 5. Associating the file with its statuses
     *
     * @param file     The MultipartFile to upload
     * @param statuses List of DocumentFileStatus entries to associate with the file
     * @return A FileWithStatusesResponse containing details of the uploaded file, including its ID and associated statuses
     * @throws ClientException if the file name is null or if there are issues during the upload process
     */
    public FileWithStatusesResponse uploadFiles(MultipartFile file, List<DocumentFileStatus> statuses) {

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "proxy_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ReconnectionOfThePowerSupplyFiles reconnectionOfThePowerSupplyFile = ReconnectionOfThePowerSupplyFiles
                .builder()
                .name(formattedFileName)
                .localFileUrl(url)
                .fileStatuses(statuses)
                .status(EntityStatus.ACTIVE)
                .build();

        ReconnectionOfThePowerSupplyFiles saved = reconnectionOfThePowerSupplyFilesRepository.saveAndFlush(reconnectionOfThePowerSupplyFile);
        return new FileWithStatusesResponse(saved, accountManagerRepository.findByUserName(saved.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Retrieves a paginated list of reconnection table entries.
     * <p>
     * This method is used to populate the reconnection table view, providing details such as:
     * - Point of Delivery (POD) information
     * - Customer details
     * - Disconnection and reconnection dates
     * - Status information
     * <p>
     * The results can be filtered and sorted based on the provided request parameters.
     *
     * @param request The request object containing parameters for filtering, pagination, and sorting of the table data
     * @return A Page of CreateReconnectionTableResponse objects representing the table entries
     */
    public Page<CreateReconnectionTableResponse> table(ReconnectionTableListingRequest request) {
        return reconnectionOfThePowerSupplyRepository
                .findTableByGridOperatorId(request.getGridOperatorId(), EPBStringUtils.fromPromptToQueryParameter(request.getPrompt())
                        , getSearchByEnum(request.getSearchBy()), PageRequest.of(request.getPage(), request.getPageSize()))
                .map(CreateReconnectionTableResponse::new);
    }

    private String getSearchByEnum(TableSearchBy searchFields) {
        return searchFields != null ? searchFields.getValue() : CustomerReceivableSearchBy.ALL.getValue();
    }

    /**
     * Uploads a proxy file (Excel format) for reconnection processing.
     * <p>
     * This method is specifically designed to handle Excel files (.xlsx) containing
     * reconnection data. It performs the following operations:
     * 1. Validates that the uploaded file is in the correct Excel format
     * 2. Generates a unique file name
     * 3. Uploads the file to a designated FTP location
     * 4. Creates a database record for the uploaded proxy file
     *
     * @param file The MultipartFile to upload (must be in .xlsx format)
     * @return A ProxyFileResponse containing details of the uploaded proxy file
     * @throws ClientException if the file is not in Excel format or if there are issues during the upload process
     */
    public ProxyFileResponse uploadProxyFile(MultipartFile file) {
        if (!hasExcelFormat(file)) {
            throw new ClientException("You can upload only .xlsx format file;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "proxy_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);


        ReconnectionOfThePowerSupplyExecutedFile proxyFile =
                ReconnectionOfThePowerSupplyExecutedFile.builder()
                        .fileUrl(url)
                        .name(fileName)
                        .status(ReceivableSubObjectStatus.ACTIVE)
                        .build();
        var savedProxyFile = reconnectionOfThePowerSupplyExecutedFileRepository.save(proxyFile);
        return new ProxyFileResponse(savedProxyFile);
    }


    private boolean hasExcelFormat(MultipartFile file) {
        return Objects.equals(file.getContentType(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Downloads a previously uploaded proxy file.
     * <p>
     * This method retrieves the file content for a proxy file associated with a reconnection.
     * It performs the following steps:
     * 1. Verifies the existence of the file and its active status
     * 2. Retrieves the file content from the storage location
     * 3. Prepares the file content for download
     *
     * @param id The ID of the proxy file to download
     * @return A FileContent object containing the file data and original file name
     * @throws DomainEntityNotFoundException if the file with the given ID is not found or is not active
     */
    public FileContent downloadProxyFile(Long id) {
        var proxyFile = reconnectionOfThePowerSupplyFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getLocalFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }

    public FileContent downloadTemplate() {
        var content = fileService.downloadFile("ftp_folder/templates/reconnection-template.xlsx");
        return new FileContent("reconnection-template.xlsx", content.getByteArray());
    }

    /**
     * Checks if a file is archived and downloads it, handling both archived and non-archived cases.
     * <p>
     * This method provides a unified approach to file downloads, considering the possibility
     * of file archival. It performs the following operations:
     * 1. Checks if the file is marked as archived
     * 2. For archived files, attempts to retrieve from the archive system
     * 3. For non-archived files, retrieves from the regular file storage
     * 4. Handles potential archival system failures by falling back to regular storage if possible
     *
     * @param id The ID of the file to check and download
     * @return A FileContent object containing the file data and name
     * @throws Exception                     if an error occurs during file retrieval from either storage or archive
     * @throws DomainEntityNotFoundException if the file is not found in either system
     */
    public FileContent checkForArchivationAndDownload(Long id, ContractFileType fileType) throws Exception {
        if (fileType == ContractFileType.GENERATED_DOCUMENT) {
            Document document = documentsRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found;"));

            ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());
            return new FileContent(document.getName(), resource.getByteArray());
        } else {
            var reconnectionFile = reconnectionOfThePowerSupplyFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));

            if (Boolean.TRUE.equals(reconnectionFile.getIsArchived())) {
                if (Objects.isNull(reconnectionFile.getLocalFileUrl())) {
                    ByteArrayResource fileContent = archivationService.downloadArchivedFile(reconnectionFile.getDocumentId(), reconnectionFile.getFileId());

                    return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
                }
            }

            var content = fileService.downloadFile(reconnectionFile.getLocalFileUrl());
            return new FileContent(reconnectionFile.getName(), content.getByteArray());
        }
    }

    /**
     * Downloads a proxy file specifically for executed reconnections.
     * <p>
     * This method is tailored for retrieving proxy files associated with reconnections
     * that are in the EXECUTED status. It includes:
     * 1. Verification of the file's existence and its association with an executed reconnection
     * 2. Retrieval of the file content from the appropriate storage location
     * 3. Preparation of the file content for download
     *
     * @param id The ID of the proxy file to download
     * @return A FileContent object containing the file data and original file name
     * @throws DomainEntityNotFoundException if the file is not found or is not associated with an executed reconnection
     */
    public FileContent downloadProxyFileForExecuted(Long id) {
        var proxyFile = reconnectionOfThePowerSupplyExecutedFileRepository.findByIdAndStatus(id, ReceivableSubObjectStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }


    private List<String> getReconnectionEntityStatuses() {
        List<String> entityStatuses = new ArrayList<>();
        entityStatuses.add(EntityStatus.ACTIVE.name());
        if (userHasPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DELETED)) {
            entityStatuses.add(EntityStatus.DELETED.name());
        }
        return entityStatuses;
    }

    private boolean userHasPermission(PermissionEnum permissionEnum) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.RECONNECTION_OF_POWER_SUPPLY, List.of(permissionEnum));
    }

    private List<String> getReconnectionModifiedStatuses(List<ReconnectionStatus> reconnectionStatuses) {
        List<String> modifiedStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(reconnectionStatuses)) {
            reconnectionStatuses.forEach(status -> {
                if (status.equals(ReconnectionStatus.DRAFT)
                        && userHasPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT)) {
                    modifiedStatuses.add(ReconnectionStatus.DRAFT.name());
                } else if (status.equals(ReconnectionStatus.EXECUTED)
                        && userHasPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED)) {
                    modifiedStatuses.add(ReconnectionStatus.EXECUTED.name());
                }
            });
        } else {
            if (userHasPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT)) {
                modifiedStatuses.add(ReconnectionStatus.DRAFT.name());
            }
            if (userHasPermission(PermissionEnum.RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED)) {
                modifiedStatuses.add(ReconnectionStatus.EXECUTED.name());
            }
        }
        return modifiedStatuses;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PodDate {
        private String podIdentifier;
        private LocalDate reconnectionDate;
    }

    /**
     * Saves associated templates for a reconnection.
     * <p>
     * This method manages the association of contract templates with a reconnection. It performs:
     * 1. Validation of the provided template IDs
     * 2. Checking if the templates are appropriate for reconnection purposes
     * 3. Creating associations between the reconnection and the valid templates
     * 4. Handling any errors encountered during the process
     *
     * @param templateIds   Set of template IDs to associate with the reconnection
     * @param psdReminderId ID of the associated PSD reminder (reconnection)
     * @param errorMessages List to collect any error messages encountered during the process
     */
    public void saveTemplates(Set<Long> templateIds, Long psdReminderId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templateIds, ContractTemplatePurposes.RECONNECTION_POWER, ContractTemplateStatus.ACTIVE);

        List<ReconnectionPowerSupplyTemplates> cbgTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            cbgTemplates.add(new ReconnectionPowerSupplyTemplates(templateId, psdReminderId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        powerSupplyTemplateRepository.saveAll(cbgTemplates);
    }

    /**
     * Updates associated templates for a reconnection.
     *
     * This method manages updates to the template associations for an existing reconnection. It includes:
     * 1. Identifying new template associations to be created
     * 2. Removing obsolete template associations
     * 3. Validating the updated set of templates
     * 4. Applying the changes to the database
     * 5. Handling any errors encountered during the update process
     *
     * @param templateIds Set of template IDs representing the updated template associations
     * @param objectionToChangeWithdrawalId ID of the associated objection to change withdrawal (reconnection)
     * @param errorMessages List to collect any error messages encountered during the process
     */
    /**
     * Updates the templates associated with a reconnection of power supply.
     * <p>
     * This method manages the updates to the template associations for an existing reconnection. It includes:
     * 1. Identifying new template associations to be created
     * 2. Removing obsolete template associations
     * 3. Validating the updated set of templates
     * 4. Applying the changes to the database
     * 5. Handling any errors encountered during the update process
     *
     * @param templateIds                   Set of template IDs representing the updated template associations
     * @param objectionToChangeWithdrawalId ID of the associated objection to change withdrawal (reconnection)
     * @param errorMessages                 List to collect any error messages encountered during the process
     */
    public void updateTemplates(Set<Long> templateIds, Long objectionToChangeWithdrawalId, List<String> errorMessages) {
        Map<Long, ReconnectionPowerSupplyTemplates> templateMap = powerSupplyTemplateRepository.findByProductDetailId(objectionToChangeWithdrawalId).stream().collect(Collectors.toMap(ReconnectionPowerSupplyTemplates::getTemplateId, j -> j));
        List<ReconnectionPowerSupplyTemplates> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            ReconnectionPowerSupplyTemplates remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new ReconnectionPowerSupplyTemplates(templateId, objectionToChangeWithdrawalId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.RECONNECTION_POWER, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<ReconnectionPowerSupplyTemplates> values = templateMap.values();
        for (ReconnectionPowerSupplyTemplates value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        powerSupplyTemplateRepository.saveAll(templatesToSave);

    }

    /**
     * Retrieves templates associated with a contract for reconnection purposes.
     * <p>
     * This method fetches and returns a list of contract templates that are:
     * 1. Associated with the given product detail (contract)
     * 2. Relevant for reconnection processes
     * 3. Active and valid as of the current date
     *
     * @param productDetailId ID of the product detail (contract) to fetch templates for
     * @return List of ContractTemplateShortResponse objects representing the associated templates
     */
    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return powerSupplyTemplateRepository.findForContract(productDetailId, LocalDate.now());
    }

    private void updatePods(Long reconnectionId) {
        List<PointOfDelivery> pods = new ArrayList<>();
        List<Long> reconnectionOfThePowerSupplyPodIds = reconnectionOfThePowerSupplyDetailedInfoRepository.findPodIdsByReconnectionId(reconnectionId);

        for (Long podId : reconnectionOfThePowerSupplyPodIds) {
            Optional<PointOfDelivery> pod = pointOfDeliveryRepository.findById(podId);
            if (pod.isPresent()) {
                pod.get().setDisconnectionPowerSupply(false);
                pods.add(pod.get());
            }
        }

        pointOfDeliveryRepository.saveAll(pods);
    }

    /**
     * Archives the files associated with a reconnection of power supply.
     * <p>
     * This method retrieves all active reconnection files for the given reconnection, downloads their content, and archives them
     * in the EDMS system. The archived files are associated with the reconnection number and other relevant metadata.
     *
     * @param reconnection The reconnection of power supply entity for which to archive the files.
     */
    private void archiveFiles(ReconnectionOfThePowerSupply reconnection) {
        Set<ReconnectionOfThePowerSupplyFiles> reconnectionFiles = reconnectionOfThePowerSupplyFilesRepository.findByReconnectionIdAndSubObjectStatus(reconnection.getId(), EntityStatus.ACTIVE);

        if (CollectionUtils.isNotEmpty(reconnectionFiles)) {
            for (ReconnectionOfThePowerSupplyFiles reconnectionFile : reconnectionFiles) {
                try {
                    reconnectionFile.setNeedArchive(true);
                    reconnectionFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_RECONNECTION_OF_THE_POWER_SUPPLY_FILE);
                    reconnectionFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_RECONNECTION_OF_THE_POWER_SUPPLY_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), reconnection.getReconnectionNumber()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(reconnectionFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: [%s]".formatted(reconnectionFile.getLocalFileUrl()), e);
                }
            }
        }
    }
}
