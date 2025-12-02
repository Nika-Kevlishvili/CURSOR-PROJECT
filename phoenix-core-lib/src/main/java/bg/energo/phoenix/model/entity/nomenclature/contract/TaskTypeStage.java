package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.TermType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "nomenclature", name = "task_type_stages")
public class TaskTypeStage extends BaseEntity {
    @Id
    @SequenceGenerator(name = "task_types_stages_seq", schema = "nomenclature", sequenceName = "task_type_stages_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_types_stages_seq")
    private Long id;

    @Column(name = "performer")
    private Long performerId;
    @Column(name = "performer_group")
    private Long performerGroupId;
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

    @Column(name = "task_type_id")
    private Long taskTypeId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
