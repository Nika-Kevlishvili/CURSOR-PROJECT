package bg.energo.phoenix.model.entity.receivable.latePaymentFine;

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
@Table(name = "late_payment_fine_ftp_files", schema = "receivable")
@Builder
public class LatePaymentFineDocumentFile extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "late_payment_fine_ftp_files_id_seq",
            sequenceName = "receivable.late_payment_fine_ftp_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "late_payment_fine_ftp_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "document_id")
    private Long documentId;

}
