package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

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
@Table(name = "power_supply_reconnection_templates", schema = "receivable")
public class ReconnectionPowerSupplyTemplates extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_reconnection_templates_id_seq",
            sequenceName = "receivable.power_supply_reconnection_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_reconnection_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "power_supply_reconnection_id")
    private Long powerSupplyReconnectionId;


    @Column(name = "template_id")
    private Long templateId;

    public ReconnectionPowerSupplyTemplates(Long templateId, Long powerSupplyReconnectionId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.powerSupplyReconnectionId=powerSupplyReconnectionId;
    }
    public ReconnectionPowerSupplyTemplates cloneEntity(Long objectionToChangeId){
        ReconnectionPowerSupplyTemplates objectionToChangeOfCbgTemplates = new ReconnectionPowerSupplyTemplates();
        objectionToChangeOfCbgTemplates.setPowerSupplyReconnectionId(powerSupplyReconnectionId);
        objectionToChangeOfCbgTemplates.setTemplateId(this.templateId);
        objectionToChangeOfCbgTemplates.setStatus(this.status);
        return objectionToChangeOfCbgTemplates;
    }
}
