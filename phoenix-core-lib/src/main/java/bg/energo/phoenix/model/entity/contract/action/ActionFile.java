package bg.energo.phoenix.model.entity.contract.action;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.List;

@SuperBuilder
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "action_files", schema = "action")
public class ActionFile extends FileArchivation {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "action_files_id_seq",
            sequenceName = "action.action_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "action_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "action.action_file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "action.action_file_status[]")
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
