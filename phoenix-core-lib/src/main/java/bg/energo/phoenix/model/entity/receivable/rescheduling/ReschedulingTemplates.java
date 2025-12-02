package bg.energo.phoenix.model.entity.receivable.rescheduling;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rescheduling_agreement_templates", schema = "receivable")
@Entity
public class ReschedulingTemplates extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "rescheduling_agreement_templates_id_seq",
            sequenceName = "receivable.rescheduling_agreement_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rescheduling_agreement_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;


    @Column(name = "template_id")
    private Long templateId;

    public ReschedulingTemplates(Long templateId, Long reschedulingId) {
        this.status = EntityStatus.ACTIVE;
        this.templateId = templateId;
        this.reschedulingId = reschedulingId;
    }
}
