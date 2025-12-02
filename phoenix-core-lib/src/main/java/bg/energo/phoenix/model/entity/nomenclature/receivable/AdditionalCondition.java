package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "additional_conditions", schema = "nomenclature")
public class AdditionalCondition extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "nomenclature_additional_conditions_id_seq",
            schema = "nomenclature",
            sequenceName = "additional_conditions_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "nomenclature_additional_conditions_id_seq"
    )
    private Long id;

    @Column(name = "customer_assessment_type_id")
    private Long customerAssessmentTypeId;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;
}
