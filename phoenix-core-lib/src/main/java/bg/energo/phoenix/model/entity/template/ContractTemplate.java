package bg.energo.phoenix.model.entity.template;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "templates", schema = "template")
@SuperBuilder
public class ContractTemplate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "templates_id_gen")
    @SequenceGenerator(name = "templates_id_gen", sequenceName = "templates_id_seq",schema = "template", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "last_template_detail_id")
    private Long lastTemplateDetailId;

    @Column(name = "template_purpose")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplatePurposes templatePurpose;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplateStatus status;

    @Column(name = "default_for_goods_order_document")
    private Boolean defaultForGoodsOrderDocument;

    @Column(name = "default_for_goods_order_email")
    private Boolean defaultForGoodsOrderEmail;

    @Column(name = "default_for_late_payment_fine_document")
    private Boolean defaultForLatePaymentFineDocument;

    @Column(name = "default_for_late_payment_fine_email")
    private Boolean defaultForLatePaymentFineEmail;

}