package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "power_supply_disconnection_tasks", schema = "receivable")
public class DisconnectionPowerSupplyTask extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_tasks_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_tasks_id_seq"
    )
    private Long id;

    @Column(name = "power_supply_disconnection_id")
    private Long powerSupplyDisconnectionId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;
}
