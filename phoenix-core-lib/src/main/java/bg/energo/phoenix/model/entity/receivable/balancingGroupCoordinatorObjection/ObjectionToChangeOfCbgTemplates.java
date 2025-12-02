package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

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
@Table(name = "objection_to_change_of_cbg_templates", schema = "receivable")
public class ObjectionToChangeOfCbgTemplates extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_templates_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "objection_to_change_of_cbg_id")
    private Long objectionToChangeId;

    @Column(name = "template_id")
    private Long templateId;

    public ObjectionToChangeOfCbgTemplates(Long templateId, Long objectionToChangeId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.objectionToChangeId=objectionToChangeId;
    }
    public ObjectionToChangeOfCbgTemplates cloneEntity(Long objectionToChangeId){
        ObjectionToChangeOfCbgTemplates objectionToChangeOfCbgTemplates = new ObjectionToChangeOfCbgTemplates();
        objectionToChangeOfCbgTemplates.setObjectionToChangeId(objectionToChangeId);
        objectionToChangeOfCbgTemplates.setTemplateId(this.templateId);
        objectionToChangeOfCbgTemplates.setStatus(this.status);
        return objectionToChangeOfCbgTemplates;
    }
}
