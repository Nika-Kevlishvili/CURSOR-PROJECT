package bg.energo.phoenix.model.entity;

import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.service.archivation.edms.FileArchivationDeleteEventListener;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(FileArchivationDeleteEventListener.class)
public abstract class FileArchivation extends BaseEntity {
    @Column(name = "archived_file_type")
    private String archivedFileType;

    @Column(name = "is_archived")
    private Boolean isArchived;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "file_id")
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    protected EntityStatus status;

    @Getter
    @Transient
    private boolean needArchive;

    @Getter
    @Transient
    private boolean signedFile;

    @Getter
    @Transient
    private EDMSArchivationConstraints archivationConstraints;

    @Getter
    @Transient
    private List<Attribute> attributes;

    public abstract Long getId();

    public abstract String getName();

    public abstract String getLocalFileUrl();

    public abstract void setLocalFileUrl(String localFileUrl);

    public abstract String getSignedFileUrl();

    public abstract void setSignedFileUrl(String signedFileUrl);

}
