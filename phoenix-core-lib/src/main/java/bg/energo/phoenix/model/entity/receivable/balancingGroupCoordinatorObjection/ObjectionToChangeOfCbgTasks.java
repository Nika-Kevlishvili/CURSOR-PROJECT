package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
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
@Table(name = "objection_to_change_of_cbg_tasks", schema = "receivable")

public class ObjectionToChangeOfCbgTasks extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_tasks_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_tasks_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "change_of_cbg_id")
    private Long changeOfCbgId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
