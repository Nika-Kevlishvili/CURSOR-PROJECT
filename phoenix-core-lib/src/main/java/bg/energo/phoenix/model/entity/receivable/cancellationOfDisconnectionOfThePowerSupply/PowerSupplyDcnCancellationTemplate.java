package bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_dcn_cancellation_templates", schema = "receivable")

public class PowerSupplyDcnCancellationTemplate extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_dcn_cancellation_templates_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellation_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellation_templates_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "power_supply_dcn_cancellation_id")
    private Long powerSupplyDcnCancellationId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
