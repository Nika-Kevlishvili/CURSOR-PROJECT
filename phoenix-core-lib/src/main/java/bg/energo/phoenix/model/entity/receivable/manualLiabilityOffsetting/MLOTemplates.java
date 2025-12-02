package bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
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
@Table(name = "mlo_email_templates", schema = "receivable")
public class MLOTemplates extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mlo_email_templates_id_seq",
            sequenceName = "receivable.mlo_email_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mlo_email_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "manual_liabilitie_offsetting_id")
    private Long mloId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableTemplateType type;

    public MLOTemplates(Long templateId, Long mloId,ReceivableTemplateType type) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.mloId=mloId;
        this.type=type;
    }
}
