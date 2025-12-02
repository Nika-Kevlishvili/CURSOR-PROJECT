package bg.energo.phoenix.model.entity.template;

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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_templates", schema = "billing")
public class BillingRunTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_templates_id_seq",
            sequenceName = "billing.billing_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "billing_id")
    private Long billingId;

    public BillingRunTemplate(Long templateId, Long billingId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.billingId=billingId;
    }
    public BillingRunTemplate cloneEntity(Long billingId){
        BillingRunTemplate billingRunTemplate = new BillingRunTemplate();
        billingRunTemplate.setBillingId(billingId);
        billingRunTemplate.setTemplateId(this.templateId);
        billingRunTemplate.setStatus(this.status);
        return billingRunTemplate;
    }
}
