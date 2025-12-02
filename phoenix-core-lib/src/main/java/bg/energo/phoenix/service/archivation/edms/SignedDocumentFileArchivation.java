package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import io.ebean.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class SignedDocumentFileArchivation {

    private final EDMSFileArchivationService archivationService;
    private final FileService fileService;
    private final EDMSAttributeProperties attributeProperties;

    @Autowired
    public SignedDocumentFileArchivation(
            EDMSFileArchivationService archivationService,
            FileService fileService,
            EDMSAttributeProperties attributeProperties
    ) {
        this.archivationService = archivationService;
        this.fileService = fileService;
        this.attributeProperties = attributeProperties;
    }

    @Transactional
    public void archiveSignedFile(Document document) {
        try {
            if (Objects.equals(document.getStatus(), EntityStatus.ACTIVE)
                    && Objects.equals(document.getDocumentStatus(), DocumentStatus.SIGNED)
                    && !document.getSigners().isEmpty()
                    && !document.getSignedBy().isEmpty()
                    && document.getSigners().equals(document.getSignedBy())) {
                if (!Boolean.TRUE.equals(document.getIsArchived())) {
                    log.debug("Archiving file");

                    log.debug("Downloading file");
                    ByteArrayResource fileContent = fileService.downloadFile(document.getSignedFileUrl());
                    log.debug("File downloaded successfully");
                    setParams(document);

                    archivationService.archiveDocuments(
                            document,
                            document.getName(),
                            fileContent.getContentAsByteArray(),
                            false,
                            document.getArchivationConstraints(),
                            document.getAttributes());

                    log.debug("File archived successfully");
                }
            }

        } catch (Exception e) {
            log.error("Exception handled while trying to archive signed file in EDMS", e);
        }
    }

    private void setParams(Document document) {
        EDMSArchivationConstraints fileType = null;
        if (document.getArchivedFileType() != null) {
            fileType = EnumUtils.getEnum(EDMSArchivationConstraints.class, document.getArchivedFileType());
        }
        document.setSignedFile(true);
        document.setNeedArchive(true);
        document.setArchivationConstraints(fileType);
        document.setAttributes(
                List.of(
                        new Attribute(attributeProperties.getDocumentTypeGuid(), document.getArchivedFileType()),
                        new Attribute(attributeProperties.getDocumentNumberGuid(), document.getName()),
                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                        new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                        new Attribute(attributeProperties.getSignedGuid(), true)
                )
        );
    }

}
