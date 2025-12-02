package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.CustomerAssessmentCriteriaType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_assessment_criterias", schema = "nomenclature")
public class CustomerAssessmentCriteria extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "nomenclature_collection_partners_id_seq",
            schema = "nomenclature",
            sequenceName = "collection_partners_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "nomenclature_collection_partners_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "value_from")
    private BigDecimal valueFrom;

    @Column(name = "value_to")
    private BigDecimal valueTo;

    @Column(name = "value")
    private Boolean value;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "is_hard_coded")
    private boolean hardCoded;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "criteria_name")
    private CustomerAssessmentCriteriaType criteriaType;
}
