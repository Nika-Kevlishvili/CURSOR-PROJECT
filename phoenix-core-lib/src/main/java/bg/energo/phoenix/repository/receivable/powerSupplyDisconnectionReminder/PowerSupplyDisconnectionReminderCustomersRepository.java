package bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderCustomers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerSupplyDisconnectionReminderCustomersRepository extends JpaRepository<PowerSupplyDisconnectionReminderCustomers,Long> {
}
