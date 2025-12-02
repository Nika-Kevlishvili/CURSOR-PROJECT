package bg.energo.phoenix.model.entity.receivable.rescheduling;

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
@Table(name = "rescheduling_tasks", schema = "receivable")

public class ReschedulingTasks extends BaseEntity {

        @Id
        @Column(name = "id")
        @SequenceGenerator(
                name = "rescheduling_tasks_id_seq",
                sequenceName = "receivable.rescheduling_tasks_id_seq",
                allocationSize = 1
        )
        @GeneratedValue(
                strategy = GenerationType.SEQUENCE,
                generator = "rescheduling_tasks_id_seq"
        )
        private Long id;

        @Column(name = "rescheduling_id")
        private Long reschedulingId;

        @Column(name = "task_id")
        private Long taskId;

        @Enumerated(EnumType.STRING)
        @Column(name = "status")
        @JdbcTypeCode(SqlTypes.NAMED_ENUM)
        private ReceivableSubObjectStatus status;
}
