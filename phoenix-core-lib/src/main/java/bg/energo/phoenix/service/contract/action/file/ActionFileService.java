package bg.energo.phoenix.service.contract.action.file;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionFile;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.action.file.ActionFileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.action.ActionFileRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionFileService {
    private final static String FOLDER_PATH = "action_files";
    private final ActionFileRepository actionFileRepository;
    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    private final EDMSFileArchivationService archivationService;
    private final DocumentsRepository documentsRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;


    /**
     * Uploads action file to FTP server and saves it to database.
     * The uploaded file does not have any connection to action yet.
     *
     * @param file       file to be uploaded
     * @param fileStatus
     * @return response with uploaded file details
     */
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> fileStatus) {
        log.debug("Uploading action file {}.", file.getName());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Action file name is null.");
            throw new IllegalArgumentsProvidedException("Action file name is null.");
        }

        String formattedFilename = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFilename);
        String path = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String url = fileService.uploadFile(file, path, fileName);

        ActionFile actionFile = new ActionFile();
        actionFile.setActionId(null);
        actionFile.setName(formattedFilename);
        actionFile.setLocalFileUrl(url);
        actionFile.setStatus(EntityStatus.ACTIVE);
        actionFile.setFileStatuses(fileStatus);
        actionFileRepository.save(actionFile);
        Optional<AccountManager> accountManager = accountManagerRepository.findByUserName(actionFile.getSystemUserId());
        String fileInfo = accountManager.map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("");

        return new FileWithStatusesResponse(actionFile, fileInfo);
    }


    /**
     * Downloads action file from FTP server.
     *
     * @param id action file id to be downloaded
     * @return response with action file content
     */
    public ActionFileContent download(Long id) {
        log.debug("Downloading action file with id {}.", id);

        ActionFile actionFile = actionFileRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("Action file not found with id %s.".formatted(id)));

        return new ActionFileContent(
                actionFile.getName(),
                fileService.downloadFile(actionFile.getLocalFileUrl()).getByteArray()
        );
    }

    public ActionFileContent checkForArchivationAndDownload(Long id, ContractFileType fileType) throws Exception {
        log.debug("Downloading action file with id {}.", id);
        if (fileType == ContractFileType.UPLOADED_FILE) {
            ActionFile actionFile = actionFileRepository
                    .findById(id)
                    .orElseThrow(() -> new IllegalArgumentsProvidedException("Action file not found with id %s.".formatted(id)));

            if (Boolean.TRUE.equals(actionFile.getIsArchived())) {
                if (Objects.isNull(actionFile.getLocalFileUrl())) {
                    ByteArrayResource fileContent = archivationService.downloadArchivedFile(actionFile.getDocumentId(), actionFile.getFileId());

                    return new ActionFileContent(
                            fileContent.getFilename(),
                            fileContent.getContentAsByteArray()
                    );
                }
            }
            return new ActionFileContent(
                    actionFile.getName(),
                    fileService.downloadFile(actionFile.getLocalFileUrl()).getByteArray()
            );
        } else {
            Document document = documentsRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found"));
            ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());
            return new ActionFileContent(document.getName(), resource.getByteArray());
        }
    }


    /**
     * Sets deleted status to outdated action files when being called from a scheduled task.
     */
    public void cleanupOutdatedFiles() {
        log.debug("Cleaning up outdated action files.");
        List<ActionFile> outdatedFiles = actionFileRepository.findByActionIdNullAndStatusIn(List.of(EntityStatus.ACTIVE));
        outdatedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
        actionFileRepository.saveAll(outdatedFiles);
    }


    /**
     * Attaches preliminarily uploaded files to action.
     *
     * @param fileIds       list of file ids to be attached
     * @param actionId      action id to which files will be attached
     * @param errorMessages list of error messages to be returned to the user
     */
    @Transactional
    public void attachFilesToAction(List<Long> fileIds, Long actionId, List<String> errorMessages) {
        log.debug("Attaching files with ids {} to action with id {}.", fileIds, actionId);

        if (CollectionUtils.isNotEmpty(fileIds)) {
            List<ActionFile> persistedFiles = actionFileRepository.findAllByIdInAndStatusIn(fileIds, List.of(EntityStatus.ACTIVE));
            List<Long> persistedFileIds = persistedFiles
                    .stream()
                    .map(ActionFile::getId)
                    .toList();

            List<ActionFile> tempList = new ArrayList<>();

            for (Long fileId : fileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    log.error("Action file with id {} not found.", fileId);
                    errorMessages.add("Action file with id %s not found.".formatted(fileId));
                    continue;
                }

                ActionFile actionFile = persistedFiles
                        .stream()
                        .filter(f -> f.getId().equals(fileId))
                        .findFirst()
                        .orElse(null);

                if (actionFile == null || actionFile.getActionId() != null) {
                    log.error("Action file with id {} is already attached to action.", fileId);
                    errorMessages.add("Action file with id %s is already attached to action.".formatted(fileId));
                    continue;
                }

                actionFile.setActionId(actionId);
                tempList.add(actionFile);
            }

            if (errorMessages.isEmpty()) {
                actionFileRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Updates action files.
     *
     * @param requestedFileIds list of file ids to be attached
     * @param actionId         action id to which files will be attached
     * @param errorMessages    list of error messages to be returned to the user
     */
    public void updateFiles(List<Long> requestedFileIds, Long actionId, List<String> errorMessages) {
        log.debug("Updating action files for action with id {}.", actionId);

        // fetch all potential files for the specific IDs provided in request
        List<ActionFile> availableFiles = requestedFileIds == null
                ? new ArrayList<>()
                : actionFileRepository.findAllByIdInAndStatusIn(requestedFileIds, List.of(EntityStatus.ACTIVE));

        List<ActionFile> persistedFiles = actionFileRepository.findByActionIdAndStatusIn(actionId, List.of(EntityStatus.ACTIVE));
        List<Long> persistedFileIds = persistedFiles
                .stream()
                .map(ActionFile::getId)
                .toList();

        if (CollectionUtils.isEmpty(requestedFileIds)) {
            persistedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
            actionFileRepository.saveAll(persistedFiles);
        } else {
            List<Long> availableFileIds = availableFiles
                    .stream()
                    .map(ActionFile::getId)
                    .toList();

            List<ActionFile> tempList = new ArrayList<>();

            for (Long fileId : requestedFileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    if (!availableFileIds.contains(fileId)) {
                        log.error("Action file with id {} not found.", fileId);
                        errorMessages.add("Action file with id %s not found.".formatted(fileId));
                        continue;
                    }

                    ActionFile actionFile = availableFiles
                            .stream()
                            .filter(f -> f.getId().equals(fileId))
                            .findFirst()
                            .orElse(null);

                    if (actionFile == null || actionFile.getActionId() != null) {
                        log.error("Action file with id {} is already attached to action.", fileId);
                        errorMessages.add("Action file with id %s is already attached to action.".formatted(fileId));
                        continue;
                    }

                    actionFile.setActionId(actionId);
                    tempList.add(actionFile);
                }
            }

            for (ActionFile file : persistedFiles) {
                if (!requestedFileIds.contains(file.getId())) {
                    file.setStatus(EntityStatus.DELETED);
                    tempList.add(file);
                }
            }

            if (errorMessages.isEmpty()) {
                actionFileRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Retrieves action files by action id.
     *
     * @param actionId action id
     * @return list of action files
     */
    public List<ContractFileResponse> getFiles(Long actionId) {
        List<ContractFileResponse> files = new ArrayList<>(actionFileRepository
                .findByActionIdAndStatusIn(actionId, List.of(EntityStatus.ACTIVE))
                .stream()
                .map(file -> new ContractFileResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList());
        List<ContractFileResponse> penaltyFiles = documentsRepository.findDocumentsForAction(actionId)
                .stream()
                .map(file -> new ContractFileResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList();
        files.addAll(penaltyFiles);
        return files;
    }

}
