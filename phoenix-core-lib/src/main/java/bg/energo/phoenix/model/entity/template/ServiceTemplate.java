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
@Table(name = "service_templates", schema = "service")
public class ServiceTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_templates_id_seq",
            sequenceName = "service.service_template_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "service_template_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductServiceTemplateType type;

    public ServiceTemplate(Long templateId, Long serviceDetailId,ProductServiceTemplateType type) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.serviceDetailId=serviceDetailId;
        this.type=type;
    }
    public ServiceTemplate cloneEntity(Long serviceDetailId){
        ServiceTemplate serviceContractTemplate = new ServiceTemplate();
        serviceContractTemplate.setServiceDetailId(serviceDetailId);
        serviceContractTemplate.setTemplateId(this.templateId);
        serviceContractTemplate.setStatus(this.status);
        return serviceContractTemplate;
    }
}
