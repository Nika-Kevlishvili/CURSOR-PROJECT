package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "nomenclature", name = "task_types")
public class TaskType extends BaseEntity {
    @Id
    @SequenceGenerator(name = "task_types_seq", schema = "nomenclature", sequenceName = "task_types_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_types_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;
}
