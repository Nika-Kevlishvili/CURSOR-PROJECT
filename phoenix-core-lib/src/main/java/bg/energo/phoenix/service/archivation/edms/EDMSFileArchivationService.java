package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.communication.edms.UploadFileResponse;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class EDMSFileArchivationService {
    private final EDMSManualArchivationService archivationService;

    public UploadFileResponse archive(FileArchivation fileArchivation,
                                      String fileName,
                                      byte[] content,
                                      boolean asNewRevision,
                                      EDMSArchivationConstraints archivedFileType,
                                      List<Attribute> attributes) throws Exception {
        log.debug("Starting archivation of file: [%s]".formatted(fileName));

        Path tempFile = Files.createTempFile("", "");

        Files.write(tempFile, content);

        UploadFileResponse archive = archivationService.uploadFile(
                fileName,
                asNewRevision,
                new FileSystemResource(tempFile),
                attributes
        );

        String archivedFileTypeName = archivedFileType == null ? fileArchivation.getArchivedFileType() : archivedFileType.getValue();

        fileArchivation.setIsArchived(true);
        fileArchivation.setArchivedFileType(archivedFileTypeName);
        fileArchivation.setDocumentId(archive.documentId());
        fileArchivation.setFileId(archive.fileId());

        return archive;
    }

    public void archiveDocuments(Document document,
                                 String fileName,
                                 byte[] content,
                                 boolean asNewRevision,
                                 EDMSArchivationConstraints archivedFileType,
                                 List<Attribute> attributes) throws Exception {
        log.debug("Starting archivation of document file: [%s]".formatted(fileName));

        Path tempFile = Files.createTempFile("", "");

        Files.write(tempFile, content);

        UploadFileResponse archive = archivationService.uploadFile(
                fileName,
                asNewRevision,
                new FileSystemResource(tempFile),
                attributes
        );

        String archivedFileTypeName = archivedFileType == null ? document.getArchivedFileType() : archivedFileType.getValue();
        document.setArchivedFileType(archivedFileTypeName);
        if (document.isSignedFile()) {
            document.setIsArchived(true);
            document.setDocumentId(archive.documentId());
            document.setFileId(archive.fileId());
        } else {
            document.setIsUnsignedArchived(true);
            document.setUnsignedDocumentId(archive.documentId());
            document.setUnsignedFileId(archive.fileId());
        }
    }

    public ByteArrayResource downloadArchivedFile(Long documentId, Long fileId) {
        return archivationService.downloadFile(documentId, fileId);
    }

    public void deleteArchivedFile(Long documentId) {
        archivationService.deleteDocument(documentId);
    }
}
