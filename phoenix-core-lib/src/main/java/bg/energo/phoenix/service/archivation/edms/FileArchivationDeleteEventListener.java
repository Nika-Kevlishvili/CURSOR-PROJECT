package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.entity.documents.Document;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
public class FileArchivationDeleteEventListener {
    private final EDMSFileArchivationService archivationService;

    public FileArchivationDeleteEventListener(EDMSFileArchivationService archivationService) {
        this.archivationService = archivationService;
    }

    @PreUpdate
    @Transactional(propagation = Propagation.MANDATORY)
    public void beforeAnyUpdate(FileArchivation archivation) {
        try {
            Boolean isArchived = archivation.getIsArchived();
            Boolean isUnsignedArchived = false;

            if (archivation instanceof Document document) {
                isUnsignedArchived = document.getIsUnsignedArchived();
            }

            log.info("Start deleting archive files for file archivation entity");
            if (Objects.equals(archivation.getStatus(), EntityStatus.DELETED)) {
                if (Boolean.TRUE.equals(isArchived) || Boolean.TRUE.equals(isUnsignedArchived)) {
                    log.info("Deleting archived file");
                    Long documentId = archivation.getDocumentId();
                    if (Objects.nonNull(documentId)) {
                        log.info("File is still archived in EDMS");
                        archivationService.deleteArchivedFile(documentId);
                        log.info("Archived file was deleted successfully");

                        archivation.setIsArchived(false);
                        archivation.setDocumentId(null);
                        archivation.setFileId(null);
                    }
                    if (archivation instanceof Document document) {
                        Long unsignedDocumentId = document.getUnsignedDocumentId();
                        if (unsignedDocumentId != null) {
                            archivationService.deleteArchivedFile(document.getUnsignedDocumentId());
                            document.setUnsignedFileId(null);
                            document.setUnsignedDocumentId(null);
                            document.setIsUnsignedArchived(false);
                        }
                    }

                    log.info("Updating file archivation entity");
                }
            }
            log.info("End deleting archive files for file archivation entity");
        } catch (Exception e) {
            log.error("Exception handled while trying to delete archived file in EDMS", e);
        }
    }
}
