package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryingFailedArchivationProcessor {
    private final FileService fileService;
    private final EDMSFileArchivationService fileArchivationService;
    private final EDMSAttributeProperties attributeProperties;

    @Transactional
    public <T extends FileArchivation> void retryArchivation(ExecutorService executor,
                                                             FileExpiration<T> expiredFile,
                                                             EDMSArchivationConstraints archivationConstraints) {
        try {
            List<T> failedArchivationFiles = expiredFile.findFailedArchivationFiles();
            log.info("Total number of failed archivation files: {}", failedArchivationFiles.size());

            for (T file : failedArchivationFiles) {
                executor.submit(() -> {
                    try {
                        List<Document> documents = new ArrayList<>();
                        boolean isArchived = file.getIsArchived() != null && file.getIsArchived();
                        EDMSArchivationConstraints constraints = archivationConstraints;
                        if (archivationConstraints == null) {
                            constraints = EnumUtils.getEnum(EDMSArchivationConstraints.class, file.getArchivedFileType());
                        }

                        if (file instanceof Document document) {
                            boolean isUnsignedArchived = document.getIsUnsignedArchived() != null && document.getIsUnsignedArchived();
                            if (!isUnsignedArchived) {
                                Document unsignedDocumentFile = (Document) file;
                                setParams(unsignedDocumentFile, constraints, false);
                                documents.add(unsignedDocumentFile);
                            }

                            if (!isArchived && document.getDocumentStatus() == DocumentStatus.SIGNED
                                    && !document.getSigners().isEmpty() && !document.getSignedBy().isEmpty()
                                    && document.getSigners().equals(document.getSignedBy())) {
                                Document signedDocumentFile = Document.copy((Document) file);
                                setParams(signedDocumentFile, constraints, true);
                                documents.add(signedDocumentFile);
                            }
                            for (Document doc : documents) {
                                String url;
                                if (doc.isSignedFile()) {
                                    url = doc.getSignedFileUrl();
                                    file.setSignedFile(true);
                                    file.setAttributes(doc.getAttributes());
                                } else {
                                    url = doc.getLocalFileUrl();
                                    file.setSignedFile(false);
                                    file.setAttributes(doc.getAttributes());
                                }

                                fileArchivationService.archiveDocuments(
                                        (Document) file,
                                        file.getName(),
                                        fileService.downloadFile(url).getContentAsByteArray(),
                                        false,
                                        constraints,
                                        file.getAttributes()
                                );
                            }
                            expiredFile.save(file);
                        } else {
                            setParams(file, constraints, false);
                            fileArchivationService.archive(
                                    file,
                                    file.getName(),
                                    fileService.downloadFile(file.getSignedFileUrl()).getContentAsByteArray(),
                                    false,
                                    constraints,
                                    file.getAttributes()
                            );
                            expiredFile.save(file);
                        }

                    } catch (Exception e) {
                        log.error("Cannot archive file ", e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Cannot archive file ", e);
        }
    }

    private void setParams(FileArchivation fileArchivation, EDMSArchivationConstraints constraints, Boolean isSignedFile) {
        fileArchivation.setSignedFile(isSignedFile);
        fileArchivation.setNeedArchive(true);
        fileArchivation.setArchivationConstraints(constraints);
        fileArchivation.setAttributes(
                List.of(
                        new Attribute(attributeProperties.getDocumentTypeGuid(), constraints),
                        new Attribute(attributeProperties.getDocumentNumberGuid(), fileArchivation.getName()),
                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                        new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                        new Attribute(attributeProperties.getSignedGuid(), isSignedFile)
                )
        );
    }
}
