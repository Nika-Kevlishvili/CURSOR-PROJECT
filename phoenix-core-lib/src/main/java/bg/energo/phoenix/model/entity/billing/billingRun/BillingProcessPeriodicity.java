package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_process_periodicity", schema = "billing")
public class BillingProcessPeriodicity extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_process_periodicity_id_seq",
            sequenceName = "billing.billing_process_periodicity_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_process_periodicity_id_seq"
    )
    private Long id;

    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "process_periodicity_id")
    private Long processPeriodicityId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public BillingProcessPeriodicity(BillingProcessPeriodicity billingProcessPeriodicity,Long billingId) {
        this.billingId = billingId;
        this.processPeriodicityId = billingProcessPeriodicity.getProcessPeriodicityId();
        this.status = billingProcessPeriodicity.getStatus();
    }
}
