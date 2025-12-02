package bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mlo_ftp_files", schema = "receivable")
@Builder
public class MLODocumentFile extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mlo_ftp_files_id_seq",
            sequenceName = "receivable.mlo_ftp_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mlo_ftp_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "manual_liabilitie_offsetting_id")
    private Long mloId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
