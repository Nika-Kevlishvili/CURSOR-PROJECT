package bg.energo.phoenix.service.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.entity.activity.SystemActivityFile;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.response.activity.SystemActivityFileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.activity.SystemActivityFileRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemActivityFileService {
    private final static String FOLDER_PATH = "activity_files";
    private final SystemActivityFileRepository systemActivityFileRepository;
    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationEventListener;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;


    /**
     * Uploads system activity file to FTP server and saves it to database.
     * The uploaded file does not have any connection to system activity yet.
     *
     * @param file file to be uploaded
     * @return response with uploaded file details
     */
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        log.debug("Uploading activity file {}.", file.getName());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Activity file name is null.");
            throw new IllegalArgumentsProvidedException("Activity file name is null.");
        }

        String formattedOriginalFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedOriginalFileName);
        String path = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String url = fileService.uploadFile(file, path, fileName);

        SystemActivityFile activityFile = new SystemActivityFile();
        activityFile.setSystemActivityId(null);
        activityFile.setName(formattedOriginalFileName);
        activityFile.setLocalFileUrl(url);
        activityFile.setStatus(EntityStatus.ACTIVE);
        activityFile.setFileStatuses(statuses);
        systemActivityFileRepository.saveAndFlush(activityFile);

        return new FileWithStatusesResponse(activityFile, accountManagerRepository.findByUserName(activityFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }


    /**
     * Downloads system activity file from FTP server.
     *
     * @param id id of the file to be downloaded
     * @return response with downloaded file details
     */
    public SystemActivityFileContent download(Long id) {
        log.debug("Downloading activity file with id {}.", id);

        SystemActivityFile activityFile = systemActivityFileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id %s not found.".formatted(id)));

        return new SystemActivityFileContent(
                activityFile.getName(),
                fileService.downloadFile(activityFile.getLocalFileUrl()).getByteArray()
        );
    }

    public SystemActivityFileContent checkForArchivationAndDownload(Long id) throws Exception {
        log.debug("Downloading activity file with id {}.", id);

        SystemActivityFile activityFile = systemActivityFileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id %s not found.".formatted(id)));

        if (Boolean.TRUE.equals(activityFile.getIsArchived())) {
            if (Objects.isNull(activityFile.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(activityFile.getDocumentId(), activityFile.getFileId());

                return new SystemActivityFileContent(
                        fileContent.getFilename(),
                        fileContent.getContentAsByteArray()
                );
            }
        }

        return new SystemActivityFileContent(
                activityFile.getName(),
                fileService.downloadFile(activityFile.getLocalFileUrl()).getByteArray()
        );
    }

    /**
     * Sets deleted status to outdated system activity files when being called from a scheduled task.
     */
    public void cleanupOutDatedFiles() {
        log.debug("Cleaning up outdated system activity files.");
        List<SystemActivityFile> outdatedFiles = systemActivityFileRepository.findBySystemActivityIdNullAndStatusIn(List.of(EntityStatus.ACTIVE));
        outdatedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
        systemActivityFileRepository.saveAll(outdatedFiles);
    }


    /**
     * Attaches preliminarily uploaded files to system activity.
     *
     * @param fileIds           list of file ids to be attached
     * @param systemActivityId  id of the system activity to which the files will be attached
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void attachFilesToSystemActivity(List<Long> fileIds, Long systemActivityId, List<String> exceptionMessages) {
        log.debug("Attaching files {} to system activity with id {}.", fileIds, systemActivityId);

        if (CollectionUtils.isNotEmpty(fileIds)) {
            List<SystemActivityFile> persistedFiles = systemActivityFileRepository.findAllByIdInAndStatusIn(fileIds, List.of(EntityStatus.ACTIVE));
            List<Long> persistedFileIds = persistedFiles
                    .stream()
                    .map(SystemActivityFile::getId)
                    .toList();

            List<SystemActivityFile> tempList = new ArrayList<>();

            for (Long fileId : fileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    log.error("File with id {} not found.", fileId);
                    exceptionMessages.add("File with id %s not found.".formatted(fileId));
                    continue;
                }

                SystemActivityFile activityFile = persistedFiles
                        .stream()
                        .filter(f -> f.getId().equals(fileId))
                        .findFirst()
                        .orElse(null);

                if (activityFile == null || activityFile.getSystemActivityId() != null) {
                    log.error("File with id {} is already attached to a system activity.", fileId);
                    exceptionMessages.add("File with id %s is already attached to a system activity.".formatted(fileId));
                    continue;
                }

                activityFile.setSystemActivityId(systemActivityId);
                tempList.add(activityFile);
            }

            if (exceptionMessages.isEmpty()) {
                systemActivityFileRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Updates files for system activity.
     *
     * @param requestFileIds    list of file ids to be attached
     * @param persistedFileIds  list of file ids persisted in database for the specific field of system activity
     * @param systemActivityId  id of the system activity to which the files will be attached
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateFiles(List<Long> requestFileIds, List<Long> persistedFileIds, Long systemActivityId, List<String> exceptionMessages) {
        log.debug("Updating files {} for system activity with id {}.", requestFileIds, systemActivityId);

        // fetch all potential files for the specific IDs provided in request
        List<SystemActivityFile> existingFiles = systemActivityFileRepository.findAllByIdInAndStatusIn(requestFileIds, List.of(EntityStatus.ACTIVE));

        // we should process files persisted for the specific field of system activity (there may be other fields with files too)
        List<SystemActivityFile> persistedFiles = systemActivityFileRepository.findAllById(persistedFileIds);

        if (CollectionUtils.isNotEmpty(requestFileIds)) {
            List<Long> existingFileIds = existingFiles.stream().map(SystemActivityFile::getId).toList();

            List<SystemActivityFile> tempList = new ArrayList<>();

            for (Long fileId : requestFileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    if (!existingFileIds.contains(fileId)) {
                        log.error("File with id %s not found.".formatted(fileId));
                        exceptionMessages.add("File with id %s not found.".formatted(fileId));
                        continue;
                    }

                    SystemActivityFile activityFile = existingFiles
                            .stream()
                            .filter(f -> f.getId().equals(fileId))
                            .findFirst()
                            .orElse(null);

                    if (activityFile == null || activityFile.getSystemActivityId() != null) {
                        log.error("File with id %s is already attached to a system activity.".formatted(fileId));
                        exceptionMessages.add("File with id %s is already attached to a system activity.".formatted(fileId));
                        continue;
                    }

                    activityFile.setSystemActivityId(systemActivityId);
                    tempList.add(activityFile);
                }
            }

            for (SystemActivityFile file : persistedFiles) {
                if (!requestFileIds.contains(file.getId())) {
                    file.setStatus(EntityStatus.DELETED);
                    tempList.add(file);
                }
            }

            if (exceptionMessages.isEmpty()) {
                systemActivityFileRepository.saveAll(tempList);
            }
        } else {
            persistedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
            systemActivityFileRepository.saveAll(persistedFiles);
        }
    }

    @Transactional
    public void archiveFiles(SystemActivity systemActivity) {
        List<SystemActivityFile> systemActivityFiles = systemActivityFileRepository.findSystemActivityFileBySystemActivityIdAndStatus(systemActivity.getId(), EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(systemActivityFiles)) {
            for (SystemActivityFile systemActivityFile : systemActivityFiles) {
                systemActivityFile.setNeedArchive(true);
                systemActivityFile.setAttributes(
                        List.of(
                                new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SYSTEM_ACTIVITY_FILE),
                                new Attribute(attributeProperties.getDocumentNumberGuid(), systemActivity.getActivityNumber()),
                                new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                new Attribute(attributeProperties.getSignedGuid(), false)
                        )
                );
                systemActivityFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SYSTEM_ACTIVITY_FILE);

                fileArchivationEventListener.archive(systemActivityFile);
            }
        }
    }

    /**
     * Retrieves system activity files by their ids.
     *
     * @param fileIds list of file ids
     * @return list of system activity files
     */
    public List<FileWithStatusesResponse> getFilesByIdsIn(List<Long> fileIds) {
        return systemActivityFileRepository
                .findAllByIdIn(fileIds)
                .stream()
                .map(file -> new FileWithStatusesResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList();
    }
}
