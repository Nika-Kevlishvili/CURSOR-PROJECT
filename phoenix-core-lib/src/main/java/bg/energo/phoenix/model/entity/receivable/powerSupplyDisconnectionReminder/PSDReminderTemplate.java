package bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder;

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
@Table(name = "power_supply_disconnection_reminder_templates", schema = "receivable")
public class PSDReminderTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_reminder_templates_id_seq",
            sequenceName = "receivable.power_supply_disconnection_reminder_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_reminder_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "power_supply_disconnection_reminder_id")
    private Long psdReminderId;


    @Column(name = "template_id")
    private Long templateId;

    public PSDReminderTemplate(Long templateId, Long psdReminderId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.psdReminderId=psdReminderId;
    }
    public PSDReminderTemplate cloneEntity(Long psdReminderId){
        PSDReminderTemplate objectionToChangeOfCbgTemplates = new PSDReminderTemplate();
        objectionToChangeOfCbgTemplates.setPsdReminderId(psdReminderId);
        objectionToChangeOfCbgTemplates.setTemplateId(this.templateId);
        objectionToChangeOfCbgTemplates.setStatus(this.status);
        return objectionToChangeOfCbgTemplates;
    }
}
