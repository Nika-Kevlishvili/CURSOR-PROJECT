package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "nomenclature", name = "action_types")
public class ActionType extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "action_types_id_seq",
            schema = "nomenclature",
            sequenceName = "action_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "action_types_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

}
