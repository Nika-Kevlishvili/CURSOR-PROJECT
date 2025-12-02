package bg.energo.phoenix.model.entity.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "late_payment_fine_tasks", schema = "receivable")
public class LatePaymentFineTask extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "late_payment_fine_tasks_id_seq",
            sequenceName = "receivable.late_payment_fine_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "late_payment_fine_tasks_id_seq"
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentFineId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableSubObjectStatus status;
}
