package bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_dcn_cancellations_doc_files", schema = "receivable")
public class CancellationDcnDocFile extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_dcn_cancellations_doc_files_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellations_doc_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellations_doc_files_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "cancellation_id")
    private Long cancellationId;

}
