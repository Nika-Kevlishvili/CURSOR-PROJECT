package bg.energo.phoenix.model.entity.task;

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

@Table(name = "task_activity", schema = "task")
public class TaskActivity extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "task_activity_id_seq",
            sequenceName = "task.task_activity_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "task_activity_id_seq"
    )
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "activity_id")
    private Long systemActivityId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}