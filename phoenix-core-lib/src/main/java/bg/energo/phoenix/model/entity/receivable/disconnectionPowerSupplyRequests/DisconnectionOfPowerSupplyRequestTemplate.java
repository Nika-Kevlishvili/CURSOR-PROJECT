package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

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
@Table(name = "power_supply_disconnection_request_templates", schema = "receivable")
public class DisconnectionOfPowerSupplyRequestTemplate extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_request_templates_id_seq",
            sequenceName = "receivable.power_supply_disconnection_request_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_request_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "power_supply_disconnection_request_id")
    private Long disconnectionId;

    @Column(name = "template_id")
    private Long templateId;

    public DisconnectionOfPowerSupplyRequestTemplate(Long templateId, Long disconnectionId) {
        this.status=EntityStatus.ACTIVE;
        this.templateId=templateId;
        this.disconnectionId=disconnectionId;
    }
    public DisconnectionOfPowerSupplyRequestTemplate cloneEntity(Long disconnectionId){
        DisconnectionOfPowerSupplyRequestTemplate disconnection = new DisconnectionOfPowerSupplyRequestTemplate();
        disconnection.setDisconnectionId(disconnectionId);
        disconnection.setTemplateId(this.templateId);
        disconnection.setStatus(this.status);
        return disconnection;
    }
}
