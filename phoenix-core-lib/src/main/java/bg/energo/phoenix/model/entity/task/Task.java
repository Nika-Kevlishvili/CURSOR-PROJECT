package bg.energo.phoenix.model.entity.task;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tasks", schema = "task")
public class Task extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "number")
    private Long number;

    @Column(name = "task_type_id")
    private Long taskTypeId;

    @Column(name = "current_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TaskStatus taskStatus;

    @Column(name = "connection_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TaskConnectionType connectionType;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}