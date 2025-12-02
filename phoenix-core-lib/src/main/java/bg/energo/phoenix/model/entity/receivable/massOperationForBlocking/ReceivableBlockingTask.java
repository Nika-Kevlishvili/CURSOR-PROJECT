package bg.energo.phoenix.model.entity.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
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
@Table(name = "mass_operation_for_blocking_tasks", schema = "receivable")
public class ReceivableBlockingTask extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mass_operation_for_blocking_tasks_id_seq",
            sequenceName = "receivable.mass_operation_for_blocking_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mass_operation_for_blocking_tasks_id_seq"
    )
    private Long id;

    @Column(name = "mass_operation_for_blocking_id")
    private Long receivableBlockingId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableSubObjectStatus status;

}
