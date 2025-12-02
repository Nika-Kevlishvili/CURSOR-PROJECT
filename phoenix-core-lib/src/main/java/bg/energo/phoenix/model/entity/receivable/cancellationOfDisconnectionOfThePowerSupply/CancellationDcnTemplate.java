package bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply;

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
@Table(name = "power_supply_dcn_cancellation_templates", schema = "receivable")
public class CancellationDcnTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_dcn_cancellation_templates_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellation_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellation_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "power_supply_dcn_cancellation_id")
    private Long powerSupplyDcnId;

    @Column(name = "template_id")
    private Long templateId;

    public CancellationDcnTemplate(Long templateId, Long powerSupplyDcnId) {
        this.status = EntityStatus.ACTIVE;
        this.templateId = templateId;
        this.powerSupplyDcnId = powerSupplyDcnId;
    }

    public CancellationDcnTemplate cloneEntity(Long powerSupplyDcnId) {
        CancellationDcnTemplate cancellation = new CancellationDcnTemplate();
        cancellation.setPowerSupplyDcnId(powerSupplyDcnId);
        cancellation.setTemplateId(this.templateId);
        cancellation.setStatus(this.status);
        return cancellation;
    }
}
