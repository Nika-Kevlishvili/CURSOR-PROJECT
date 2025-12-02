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
@Table(name = "power_supply_dcn_cancellation_tasks", schema = "receivable")

public class PowerSupplyDcnCancellationTask extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_dcn_cancellation_tasks_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellation_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellation_tasks_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "power_supply_dcn_cancellation_id")
    private Long powerSupplyDcnCancellationId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
