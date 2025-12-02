package bg.energo.phoenix.model.entity.receivable.deposit;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_deposit_email_templates", schema = "receivable")
public class DepositTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_deposit_email_templates_id_seq",
            sequenceName = "receivable.customer_deposit_email_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_deposit_email_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "customer_deposit_id")
    private Long depositId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "name")
    private String fileName;

    @Column(name = "template_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableTemplateType type;

    public DepositTemplate(Long templateId, Long depositId,ReceivableTemplateType type) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.depositId=depositId;
        this.type=type;
    }
}
