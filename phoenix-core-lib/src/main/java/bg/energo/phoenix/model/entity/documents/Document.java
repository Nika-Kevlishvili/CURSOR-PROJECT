package bg.energo.phoenix.model.entity.documents;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.util.document.DocumentSignerWrapper;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ToString
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document", schema = "template")
@SuperBuilder
public class Document extends FileArchivation {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "document_id_seq",
            schema = "template",
            sequenceName = "document_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "document_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "unsigned_file_url", length = 256)
    private String unsignedFileUrl;

    @Column(name = "signed_file_url", length = 256)
    private String signedFileUrl;


    @Column(name = "signers", columnDefinition = "template.document_signers[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.document_signers"
            )
    )
    private List<DocumentSigners> signers;

    @Column(name = "signed_by", columnDefinition = "template.document_signers[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.document_signers"
            )
    )
    @Getter(AccessLevel.NONE)
    private List<DocumentSigners> signedBy;


    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "document_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;

    @Column(name = "file_format")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private FileFormat fileFormat;

    private LocalDateTime statusModifyDate;

    @Column(name = "is_unsigned_archived")
    private Boolean isUnsignedArchived;

    @Column(name = "unsigned_document_id")
    private Long unsignedDocumentId;

    @Column(name = "unsigned_file_id")
    private Long unsignedFileId;

    public static Document copy(Document document) {
        Document copiedDoc = new Document();
        copiedDoc.setName(document.getName());
        copiedDoc.setUnsignedFileUrl(document.getUnsignedFileUrl());
        copiedDoc.setSignedFileUrl(document.getSignedFileUrl());
        if (document.getSigners() != null) {
            copiedDoc.setSigners(new ArrayList<>(document.getSigners()));
        }
        if (document.getSignedBy() != null) {
            copiedDoc.setSignedBy(new ArrayList<>(document.getSignedBy()));
        }
        copiedDoc.setTemplateId(document.getTemplateId());
        copiedDoc.setDocumentStatus(document.getDocumentStatus());
        copiedDoc.setFileFormat(document.getFileFormat());
        copiedDoc.setStatusModifyDate(document.getStatusModifyDate());
        copiedDoc.setIsUnsignedArchived(document.getIsUnsignedArchived());
        copiedDoc.setUnsignedDocumentId(document.getUnsignedDocumentId());
        copiedDoc.setUnsignedFileId(document.getUnsignedFileId());
        copiedDoc.setArchivedFileType(document.getArchivedFileType());
        copiedDoc.setIsArchived(document.getIsArchived());
        copiedDoc.setDocumentId(document.getDocumentId());
        copiedDoc.setFileId(document.getFileId());
        copiedDoc.setStatus(document.getStatus());
        copiedDoc.setNeedArchive(document.isNeedArchive());
        copiedDoc.setSignedFile(document.isSignedFile());
        copiedDoc.setArchivationConstraints(document.getArchivationConstraints());
        if (document.getAttributes() != null) {
            copiedDoc.setAttributes(new ArrayList<>(document.getAttributes()));
        }

        return copiedDoc;
    }

    @Override
    public String getLocalFileUrl() {
        return unsignedFileUrl;
    }

    @Override
    public void setLocalFileUrl(String localFileUrl) {
        this.unsignedFileUrl = localFileUrl;
    }

    public List<DocumentSigners> getSignedBy() {
        return new DocumentSignerWrapper<>(this.signedBy, this);
    }

}
