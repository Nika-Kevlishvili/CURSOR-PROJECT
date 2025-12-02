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
@Table(name = "billing_tasks", schema = "billing")
public class BillingRunTasks extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_tasks_id_seq",
            sequenceName = "billing.billing_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_tasks_id_seq"
    )
    private Long id;

    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public BillingRunTasks(Long billingId, BillingRunTasks tasks) {
        this.billingId = billingId;
        this.taskId = tasks.getTaskId();
        this.status = tasks.getStatus();
    }
}
