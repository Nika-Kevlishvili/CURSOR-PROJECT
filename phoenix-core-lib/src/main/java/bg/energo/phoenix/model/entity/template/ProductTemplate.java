package bg.energo.phoenix.model.entity.template;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
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
@Table(name = "product_templates", schema = "product")
public class ProductTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "product_templates_id_seq",
            sequenceName = "product.product_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "product_template_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductServiceTemplateType type;

    public ProductTemplate(Long templateId, Long productDetailId,ProductServiceTemplateType type) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.productDetailId=productDetailId;
        this.type=type;
    }
}
