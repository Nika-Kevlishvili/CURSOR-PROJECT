package bg.energo.phoenix.model.entity.crm.smsCommunication;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_communication_files", schema = "crm")
public class SmsCommunicationFiles extends FileArchivation {
    @Id
    @SequenceGenerator(
            name = "sms_communication_files_id_seq",
            sequenceName = "crm.sms_communication_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_communication_files_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "sms_communication_id")
    private Long smsCommunicationId;

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

    @Column(name = "is_report")
    private boolean isReport;

    @Column(name = "is_after_send_report")
    private boolean afterSendReport;

    @Override
    public String getSignedFileUrl() {
        return localFileUrl;
    }

    @Override
    public void setSignedFileUrl(String signedFileUrl) {
        this.localFileUrl = signedFileUrl;
    }

}
