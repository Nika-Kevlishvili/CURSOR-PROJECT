package bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
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
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_disconnection_reminder_tasks", schema = "receivable")
public class PowerSupplyDisconnectionReminderTasks extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_disconnection_reminder_tasks_id_seq",
            sequenceName = "receivable.power_supply_disconnection_reminder_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_reminder_tasks_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "power_supply_disconnection_reminder_id")
    private Long reminderId;

    @Column(name = "task_id")
    private Long taskId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReceivableSubObjectStatus status;
}
