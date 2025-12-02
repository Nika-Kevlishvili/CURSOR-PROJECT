package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_communication_files", schema = "crm")
public class EmailCommunicationFile extends FileArchivation {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_files_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_files_id_seq",
            sequenceName = "crm.email_communication_files_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Size(max = 200)
    @NotNull
    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "email_communication_id")
    private Long emailCommunicationId;

    @Column(name = "is_report")
    private boolean isReport;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "crm.file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "crm.file_status[]")
    private List<DocumentFileStatus> fileStatuses;

    @Override
    public String getSignedFileUrl() {
        return localFileUrl;
    }

    @Override
    public void setSignedFileUrl(String signedFileUrl) {
        this.localFileUrl = signedFileUrl;
    }

}