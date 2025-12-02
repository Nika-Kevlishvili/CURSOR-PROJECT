package bg.energo.phoenix.model.entity.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_assessment_parameters", schema = "receivable")
public class CustomerAssessmentParameters extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_assessment_parameters_id_seq",
            schema = "receivable",
            sequenceName = "customer_assessment_parameters_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_assessment_parameters_id_seq"
    )
    private Long id;

    @Column(name = "value")
    private String value;

    @Column(name = "customer_assessment_id")
    private Long customerAssessmentId;

    @Column(name = "assessment")
    private boolean assessment;

    @Column(name = "final_assessment")
    private boolean finalAssessment;

    @Column(name = "customer_assessment_criteria_id")
    private Long customerAssessmentCriteriaId;

}
