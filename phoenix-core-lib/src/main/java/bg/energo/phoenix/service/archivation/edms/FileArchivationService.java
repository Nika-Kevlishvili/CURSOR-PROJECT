package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.request.communication.edms.UploadFileResponse;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileArchivationService {
    private final EDMSFileArchivationService archivationService;
    private final FileService fileService;

    public void archive(FileArchivation archivation) {
        log.debug("Archiving file");
        if (!Boolean.TRUE.equals(archivation.getIsArchived())) {
            log.debug("File is not archived yet");

            log.debug("File need to be archived: {}", archivation.isNeedArchive());
            if (archivation.isNeedArchive()) {
                try {

                    log.debug("Downloading file");
                    ByteArrayResource fileContent = fileService.downloadFile(archivation.getLocalFileUrl());
                    log.debug("File downloaded successfully");

                    log.debug("Archiving file");
                    UploadFileResponse archive = archivationService.archive(
                            archivation,
                            archivation.getName(),
                            fileContent.getContentAsByteArray(),
                            false,
                            archivation.getArchivationConstraints(),
                            archivation.getAttributes()
                    );

                    log.debug("File archived successfully");
                } catch (Exception e) {
                    log.error("Cannot archive file", e);
                }
            }
        }
    }

    public void archiveDocument(Document document) {
        log.debug("Archiving Document file");
        if (!Boolean.TRUE.equals(document.getIsArchived()) || !Boolean.TRUE.equals(document.getIsUnsignedArchived())) {
            log.debug("Document File is not archived yet");

            log.debug("Document File need to be archived: {}", document.isNeedArchive());
            if (document.isNeedArchive()) {
                try {
                    String fileUrl = document.isSignedFile() ? document.getSignedFileUrl() : document.getLocalFileUrl();

                    log.debug("Downloading Document file");
                    ByteArrayResource fileContent = fileService.downloadFile(fileUrl);
                    log.debug("Document File downloaded successfully");

                    log.debug("Archiving Document file");
                    archivationService.archiveDocuments(
                            document,
                            document.getName(),
                            fileContent.getContentAsByteArray(),
                            false,
                            document.getArchivationConstraints(),
                            document.getAttributes()
                    );

                    log.debug("Document File archived successfully");
                } catch (Exception e) {
                    log.error("Cannot archive Document file", e);
                }
            }
        }
    }

    public ByteArrayResource download(FileArchivation fileArchivation) {
        log.debug("Downloading file");

        if (fileArchivation == null) {
            log.error("File archivation cannot be null");
            throw new IllegalArgumentException("File archivation cannot be null");
        }

        if (Boolean.TRUE.equals(fileArchivation.getIsArchived())) {
            log.debug("File is archived");
            return archivationService.downloadArchivedFile(fileArchivation.getDocumentId(), fileArchivation.getFileId());
        } else {
            log.debug("File is not archived yet");
            return fileService.downloadFile(fileArchivation.getLocalFileUrl());
        }
    }
}

