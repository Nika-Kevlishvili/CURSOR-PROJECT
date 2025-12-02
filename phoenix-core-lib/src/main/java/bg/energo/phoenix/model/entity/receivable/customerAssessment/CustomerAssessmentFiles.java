package bg.energo.phoenix.model.entity.receivable.customerAssessment;

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

@Entity
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_assessment_files", schema = "receivable")
public class CustomerAssessmentFiles extends FileArchivation {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_assessment_files_id_seq",
            schema = "receivable",
            sequenceName = "customer_assessment_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_assessment_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "customer_assessment_id")
    private Long customerAssessmentId;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "receivable.file_status[]")
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
