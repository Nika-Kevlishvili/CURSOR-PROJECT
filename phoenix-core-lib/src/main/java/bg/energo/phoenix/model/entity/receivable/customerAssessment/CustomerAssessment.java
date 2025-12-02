package bg.energo.phoenix.model.entity.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
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
@Table(name = "customer_assessments", schema = "receivable")
public class CustomerAssessment extends BaseEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "assessment_number")
    private String assessmentNumber;

    @Column(name = "assessment_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AssessmentStatus assessmentStatus;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "final_assessment")
    private Boolean finalAssessment;

    @Column(name = "customer_assessment_type_id")
    private Long customerAssessmentTypeId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
