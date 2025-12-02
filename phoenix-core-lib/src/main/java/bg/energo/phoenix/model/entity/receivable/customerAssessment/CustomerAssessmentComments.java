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
@Table(name = "customer_assessment_comments", schema = "receivable")
public class CustomerAssessmentComments extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_assessment_comments_id_seq",
            schema = "receivable",
            sequenceName = "customer_assessment_comments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_assessment_comments_id_seq"
    )
    private Long id;

    @Column(name = "comment")
    private String comment;

    @Column(name = "customer_assessment_id")
    private Long customerAssessmentId;

}
