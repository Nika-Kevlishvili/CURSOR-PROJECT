package bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_disconnection_reminder_customers", schema = "receivable")

public class PowerSupplyDisconnectionReminderCustomers extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "power_supply_disconnection_reminder_customers_id_seq",
            sequenceName = "receivable.power_supply_disconnection_reminder_customers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_reminder_customers_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_liability_id")
    private Long customerLiabilityId;

    @Column(name = "liability_amount")
    private BigDecimal liabilityAmount;

    @Column(name = "power_supply_disconnection_reminder_id")
    private Long powerSupplyDisconnectionReminderId;

}
