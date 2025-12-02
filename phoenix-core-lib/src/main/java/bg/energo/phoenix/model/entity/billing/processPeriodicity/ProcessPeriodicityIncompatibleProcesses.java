package bg.energo.phoenix.model.entity.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "process_periodicity_incompatible_billings", schema = "billing")
@Entity
public class ProcessPeriodicityIncompatibleProcesses extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_periodicity_incompatible_billings_id_seq",
            sequenceName = "billing.process_periodicity_incompatible_billings_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_periodicity_incompatible_billings_id_seq"
    )
    private Long id;

    @Column(name = "process_periodicity_id")
    private Long processPeriodicityId;

    @Column(name = "incompatible_billing_id")
    private Long incompatibleBillingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
