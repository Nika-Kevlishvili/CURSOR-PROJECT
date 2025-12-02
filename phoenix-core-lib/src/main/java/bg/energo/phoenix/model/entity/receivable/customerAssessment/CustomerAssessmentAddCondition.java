package bg.energo.phoenix.model.entity.receivable.customerAssessment;

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
@Table(name = "customer_assessment_add_conditions", schema = "receivable")
public class CustomerAssessmentAddCondition extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_assessment_add_conditions_id_seq",
            schema = "receivable",
            sequenceName = "customer_assessment_add_conditions_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_assessment_add_conditions_id_seq"
    )
    private Long id;

    @Column(name = "customer_assessment_id")
    private Long customerAssessmentId;

    @Column(name = "additional_condition_id")
    private Long additionalConditionId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
