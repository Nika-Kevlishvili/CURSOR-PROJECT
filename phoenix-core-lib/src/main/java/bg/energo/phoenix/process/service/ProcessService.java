package bg.energo.phoenix.process.service;

import bg.energo.phoenix.event.EventFactory;
import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessFile;
import bg.energo.phoenix.process.model.entity.ProcessNotification;
import bg.energo.phoenix.process.model.enums.*;
import bg.energo.phoenix.process.model.request.EditProcessRequest;
import bg.energo.phoenix.process.model.request.ProcessListRequest;
import bg.energo.phoenix.process.model.request.ProcessNotificationObject;
import bg.energo.phoenix.process.model.response.*;
import bg.energo.phoenix.process.repository.ProcessFileRepository;
import bg.energo.phoenix.process.repository.ProcessNotificationRepository;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.excel.MultiSheetExcelService;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.kafka.RabbitMQProducerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PROCESS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessFileRepository processFileRepository;
    private final ProcessMapper processMapper;
    private final MultiSheetExcelService multiSheetExcelService;
    private final PermissionService permissionService;
    private final EventFactory eventFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FileService fileService;
    private final ProcessNotificationRepository processNotificationRepository;
    private final PortalTagRepository portalTagRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final RabbitMQProducerService rabbitMQProducerService;

    /**
     * Retrieves a page of {@link ProcessListResponse} objects based on the specified request parameters.
     * Results are sorted by ID in descending order if not provided other values. Search is executed in all fields unless specified otherwise.
     *
     * @param request an object of {@link ProcessListRequest} containing search and filter criteria for the process list.
     * @return a {@link Page} of {@link ProcessListResponse} objects representing the processes that match the search criteria.
     */
    public Page<ProcessListResponse> list(@RequestParam ProcessListRequest request) {
        log.debug("Fetching process list for following request : {}", request);

        String sortBy = ProcessTableColumn.ID.getValue();
        if (request.getSortBy() != null && request.getSortBy().getValue() != null) {
            sortBy = request.getSortBy().getValue();
        }

        String searchBy = ProcessSearchField.ALL.getValue();
        if (request.getSortBy() != null && request.getSortBy().getValue() != null) {
            searchBy = ProcessSearchField.ALL.getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String systemUserId = null;
        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_VIEW_SU.getId())) {
            systemUserId = permissionService.getLoggedInUserId();
        }

        Page<Process> result = processRepository
                .findAll(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        searchBy,
                        request.getStatus(),
                        request.getCreatedDateFrom(),
                        request.getCreatedDateTo(),
                        request.getStartDateFrom(),
                        request.getStartDateTo(),
                        request.getCompleteDateFrom(),
                        request.getCompleteDateTo(),
                        systemUserId,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                );
        return result.map(processMapper::entityToListResponse);
    }

    /**
     * Retrieves a {@link Process} by its ID.
     *
     * @param id the ID of the {@link Process} to retrieve
     * @return a {@link ProcessResponse} object representing the retrieved process
     * @throws DomainEntityNotFoundException if the {@link Process} is not found in the database
     */
    public ProcessResponse getById(Long id) {
        log.debug("Fetching process by ID: {}", id);
        Process result = processRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + id));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_VIEW_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(result.getSystemUserId())) {
                log.error("User {} does not have enough permissions to view process {}", permissionService.getLoggedInUserId(), id);
                throw new ClientException("User " + loggedInUserId + " does not have enough permissions to view process " + id, ErrorCode.ACCESS_DENIED);
            }
        }
        List<ProcessNotificationResponse> responseByProcessId = processNotificationRepository.findResponseByProcessId(id);

        ProcessResponse processResponse = processMapper.entityToResponse(result, responseByProcessId);
        processResponse.setFileResponse(processFileRepository.getProcessFilesByProcessId(id));
        processResponse.setMassImportFileName(getProcessMassImportFileNameIfExists(id));

        return processResponse;
    }

    @Transactional
    public Long editProcess(Long id, EditProcessRequest request) {
        if (!processRepository.existsByIdAndStatusNotIn(id, List.of(ProcessStatus.COMPLETED))) {
            throw new DomainEntityNotFoundException("Process not found, ID: " + id);
        }
        // TODO: 20.02.23 implement logic later (implying editing notification preferences)
        List<ProcessNotification> processNotifications = processNotificationRepository.findAllByProcessId(id);
        Map<Triple<ProcessNotificationType, PerformerType, Long>, ProcessNotification> collect = processNotifications.stream().collect(Collectors.toMap(x -> Triple.of(x.getNotificationType(), x.getPerformerId() == null ? PerformerType.TAG : PerformerType.MANAGER, x.getProcessId() == null ? x.getPerformerTagId() : x.getPerformerId()), j -> j));
        Set<ProcessNotificationObject> notifications = request.getNotifications();
        List<ProcessNotification> notificationsToSave = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int i = 0;
        for (ProcessNotificationObject notification : notifications) {
            PerformerType performerType = notification.getPerformerType();
            ProcessNotificationType type = notification.getType();
            ProcessNotification remove = collect.remove(Triple.of(type, performerType, notification.getPerformerId()));
            if (remove == null) {
                Long performerManagerId = getPerformerManager(notification.getPerformerId(), notification.getPerformerType(), i);
                Long performerTagId = getPerformerTag(notification.getPerformerId(), notification.getPerformerType(), i);
                notificationsToSave.add(new ProcessNotification(null, id, performerTagId, performerManagerId, type));
            }
        }
        processNotificationRepository.saveAll(notificationsToSave);
        processNotificationRepository.deleteAll(collect.values());
        return id;
    }

    private Long getPerformerTag(Long performerId, PerformerType performerType, int i) {
        if (performerType.equals(PerformerType.MANAGER)) {
            return null;
        }
        if (!portalTagRepository.existsPortalTagForGroup(performerId, EntityStatus.ACTIVE)) {
            throw new DomainEntityNotFoundException("notifications[%s].performerId-Performer does not exist!;".formatted(i));
        }
        return performerId;
    }

    private Long getPerformerManager(Long performerId, PerformerType performerType, int i) {
        if (performerType.equals(PerformerType.TAG)) {
            return null;
        }
        if (!accountManagerRepository.existsByIdAndStatusIn(performerId, List.of(Status.ACTIVE))) {
            throw new DomainEntityNotFoundException("notifications[%s].performerId-Performer does not exist!;".formatted(i));
        }
        return performerId;
    }

    @Transactional
    public void startProcess(Long id) {
        log.debug("Trying to start process: {}", id);

        Process result = processRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + id));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_START_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(result.getSystemUserId())) {
                log.error("User {} does not have enough permissions to start process {}", permissionService.getLoggedInUserId(), id);
                throw new ClientException("User " + loggedInUserId + " does not have enough permissions to start process " + id, ErrorCode.ACCESS_DENIED);
            }
        }

        // TODO: 20.02.23 rest of the validations that will be implemented later
        throw new ClientException("This operation is not supported yet", ErrorCode.UNSUPPORTED_OPERATION);
    }

    /**
     * Attempts to pause the {@link Process} with the specified ID.
     *
     * @param id the ID of the process to pause
     * @throws DomainEntityNotFoundException if no process with the specified ID is found
     * @throws ClientException               if the logged-in user does not have sufficient permissions to pause the process
     * @throws OperationNotAllowedException  if the process is not currently in progress and cannot be paused
     */
    @Transactional
    public void pauseProcess(Long id) {
        log.debug("Trying to pause process: {}", id);

        Process result = processRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + id));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_PAUSE_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(result.getSystemUserId())) {
                log.error("User {} does not have enough permissions to pause process {}", permissionService.getLoggedInUserId(), id);
                throw new ClientException("User " + loggedInUserId + " does not have enough permissions to pause process " + id, ErrorCode.ACCESS_DENIED);
            }
        }

        ProcessStatus processStatus = result.getStatus();
        if (!processStatus.equals(ProcessStatus.IN_PROGRESS)) {
            log.error("Cannot pause process: {}, process is: {}", id, processStatus);
            throw new OperationNotAllowedException("Cannot pause process: " + id + ", process is: " + processStatus);
        }

        result.setStatus(ProcessStatus.PAUSED);
        processRepository.save(result);
    }

    /**
     * Attempts to resume the {@link Process} with the specified ID.
     *
     * @param id the ID of the process to resume
     * @throws DomainEntityNotFoundException if no process with the specified ID is found
     * @throws ClientException               if the logged-in user does not have sufficient permissions to resume the process
     * @throws OperationNotAllowedException  if the process is not currently paused and cannot be resumed
     */
    @Transactional
    public void resumeProcess(Long id) throws OperationNotSupportedException {
        log.debug("Trying to resume process: {}", id);

        Process result = processRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + id));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_PAUSE_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(result.getSystemUserId())) {
                log.error("User {} does not have enough permissions to resume process {}", permissionService.getLoggedInUserId(), id);
                throw new ClientException("User " + loggedInUserId + " does not have enough permissions to resume process " + id, ErrorCode.ACCESS_DENIED);
            }
        }

        ProcessStatus processStatus = result.getStatus();
        if (!processStatus.equals(ProcessStatus.PAUSED)) {
            log.error("Cannot resume process: {}, process is: {}", id, processStatus);
            throw new OperationNotAllowedException("Cannot resume process: " + id + ", process is: " + processStatus);
        }

        result.setStatus(ProcessStatus.RESUMED);
        processRepository.save(result);
        rabbitMQProducerService.publishProcessEvent( eventFactory.createProcessCreatedEvent(
                EventType.eventTypeFromProcessType(result.getType()),
                result
        ));
//        applicationEventPublisher.publishEvent(
//                eventFactory.createProcessCreatedEvent(
//                        EventType.eventTypeFromProcessType(result.getType()),
//                        result
//                )
//        );
    }

    /**
     * Attempts to cancel the {@link Process} with the specified ID.
     *
     * @param id the ID of the process to cancel
     * @throws DomainEntityNotFoundException if no process with the specified ID is found
     * @throws ClientException               if the logged-in user does not have sufficient permissions to cancel the process
     * @throws OperationNotAllowedException  if the process cannot be canceled
     */
    public void cancelProcess(Long id) {
        log.debug("Trying to cancel process: {}", id);

        Process result = processRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + id));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_CANCEL_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(result.getSystemUserId())) {
                log.error("User {} does not have enough permissions to cancel process {}", permissionService.getLoggedInUserId(), id);
                throw new ClientException("User " + loggedInUserId + " does not have enough permissions to cancel process " + id, ErrorCode.ACCESS_DENIED);
            }
        }

        ProcessStatus processStatus = result.getStatus();
        if (processStatus.equals(ProcessStatus.CANCELED)) {
            log.error("Cannot cancel process: {}, process is: {}", id, processStatus);
            throw new OperationNotAllowedException("Cannot cancel process: " + id + ", process is: " + processStatus);
        }

        result.setStatus(ProcessStatus.CANCELED);
        processRepository.save(result);
    }

    /**
     * Creates a new {@link Process} with the specified {@link ProcessType}, {@link ProcessStatus}, file URL, and user permissions.
     *
     * @param type        the type of the process
     * @param status      the status of the process
     * @param fileUrl     the URL of the stored file associated with the process
     * @param permissions permissions of the initiator user
     * @return the newly created process
     */
    @Transactional
    public Process createProcess(ProcessType type, ProcessStatus status, String fileUrl, String permissions, LocalDate date, Long collectionChannelId, Long paymentPackageId, Boolean currencyFromCollectionChannel) {
        Process process = new Process();
        process.setType(type);
        process.setCurrencyFromCollectionChannel(currencyFromCollectionChannel);
        process.setStatus(status);
        process.setFileUrl(fileUrl);
        process.setUserPermissions(permissions);
        processRepository.saveAndFlush(process);
        process.setName(String.format("%s_%s", process.getType(), process.getId()));
        process.setDate(date);
        if (collectionChannelId != null) {
            process.setCollectionChannelId(collectionChannelId);
        }
        if (paymentPackageId != null) {
            process.setPaymentPackageId(paymentPackageId);
        }
        return processRepository.save(process);
    }

    /**
     * Downloads a process report for the given process ID, in the form of a multi-sheet Excel file, to the given
     * HTTP response.
     *
     * @param processId the ID of the process to generate the report for
     * @param response  the HTTP response to write the generated Excel file to
     * @throws DomainEntityNotFoundException if no process found with the given ID or no multi-sheet Excel service is available for the given report type
     */
    public void downloadProcessReport(Long processId, String multiSheetExcelType, HttpServletResponse response) {
        log.debug("Starting to download report for process: {}", processId);

        Process process = processRepository
                .findById(processId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Process not found, ID: " + processId));

        if (!permissionService.getPermissionsFromContext(PROCESS).contains(PROCESS_VIEW_SU.getId())) {
            String loggedInUserId = permissionService.getLoggedInUserId();
            if (StringUtils.isEmpty(loggedInUserId) || !loggedInUserId.equals(process.getSystemUserId())) {
                log.debug("User {} does not have enough permissions to download error report for process {}",
                        permissionService.getLoggedInUserId(), processId);
                throw new ClientException("User %s does not have enough permissions to download error report for process %s".formatted(loggedInUserId, process), ErrorCode.ACCESS_DENIED);
            }
        }

        long totalStartTime = System.currentTimeMillis();
        multiSheetExcelService.generateExcel(multiSheetExcelType, response, String.valueOf(processId));
        long totalFinishTime = System.currentTimeMillis() - totalStartTime;

        System.out.println("Total process took: " + TimeUnit.MILLISECONDS.toSeconds(totalFinishTime) + " seconds");
    }

    public ProcessFileContent download(Long id) {
        ProcessFile processFile = processFileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(processFile.getFileUrl());

        return new ProcessFileContent(processFile.getName(), resource.getByteArray());
    }

    public ProcessFileContent downloadMassImportFile(Long processId) {
        log.debug("Starting to download uploaded mass import file for process: {}", processId);

        Process process = processRepository
                .findById(processId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Mass Import File with presented id not found;"));

        String fileName = process.getFileUrl().substring(process.getFileUrl().lastIndexOf("/") + 1);
        ByteArrayResource resource = fileService.downloadFile(process.getFileUrl());

        return new ProcessFileContent(fileName, resource.getByteArray());
    }

    private String getProcessMassImportFileNameIfExists(Long processId) {
        Process process = processRepository
                .findById(processId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Mass Import File with presented id not found;"));
        String formattedFileName = null;
        if (process.getFileUrl() != null) {
            String fileName = process.getFileUrl().substring(process.getFileUrl().lastIndexOf("/") + 1);

            try {
                formattedFileName = fileName.substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
            } catch (Exception e) {
                formattedFileName = fileName;
            }
        }
        return formattedFileName;
    }

}
