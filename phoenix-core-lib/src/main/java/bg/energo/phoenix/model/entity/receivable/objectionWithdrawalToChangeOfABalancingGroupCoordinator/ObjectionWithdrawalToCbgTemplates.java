package bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

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
@Table(name = "objection_withdrawal_to_change_of_cbg_templates", schema = "receivable")
public class ObjectionWithdrawalToCbgTemplates extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "objection_withdrawal_to_change_of_cbg_templates_id_seq",
            sequenceName = "receivable.objection_withdrawal_to_change_of_cbg_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_withdrawal_to_change_of_cbg_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "objection_withdrawal_to_change_of_cbg_id")
    private Long objectionToChangeWithdrawalId;


    @Column(name = "template_id")
    private Long templateId;

    public ObjectionWithdrawalToCbgTemplates(Long templateId, Long objectionToChangeWithdrawalId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.objectionToChangeWithdrawalId=objectionToChangeWithdrawalId;
    }
    public ObjectionWithdrawalToCbgTemplates cloneEntity(Long objectionToChangeId){
        ObjectionWithdrawalToCbgTemplates objectionToChangeOfCbgTemplates = new ObjectionWithdrawalToCbgTemplates();
        objectionToChangeOfCbgTemplates.setObjectionToChangeWithdrawalId(objectionToChangeWithdrawalId);
        objectionToChangeOfCbgTemplates.setTemplateId(this.templateId);
        objectionToChangeOfCbgTemplates.setStatus(this.status);
        return objectionToChangeOfCbgTemplates;
    }
}
