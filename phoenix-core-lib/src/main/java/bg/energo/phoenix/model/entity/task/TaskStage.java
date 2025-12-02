package bg.energo.phoenix.model.entity.task;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.enums.task.TaskStageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task_stages", schema = "task")
public class TaskStage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_stage_seq")
    @SequenceGenerator(name = "task_stage_seq", sequenceName = "task_stages_id_seq", schema = "task", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "performer")
    private Long performer;

    @Column(name = "tag_performer")
    private Long performerGroup;

    @Column(name = "performer_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PerformerType performerType;

    @Column(name = "term")
    private Long term;

    @Column(name = "term_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TermType termType;

    @Column(name = "stage")
    private Integer stage;

    @Column(name = "task_type_stage_id")
    private Long taskTypeStageId;

    @Column(name = "current_performer_id")
    private Long currentPerformerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TaskStageStatus taskStageStatus;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;
}