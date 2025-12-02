package bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PowerSupplyDcnReminderDocFileRepository extends JpaRepository<PowerSupplyDcnReminderDocFiles, Long> {
    @Query(value = """
            select remfiles.id + 1
                   from receivable.power_supply_disconnection_reminder_doc_files remfiles
                   order by remfiles.id desc
                   limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    List<PowerSupplyDcnReminderDocFiles> findPowerSupplyDcnReminderDocFilesByReminderForDcnIdAndStatus(Long reminderForDcnId, EntityStatus status);
}
