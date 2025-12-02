package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderPeriodicity;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReminderPeriodicityRepository extends JpaRepository<ReminderPeriodicity, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(processPeriodicity.id,processPeriodicity.name)
            from ReminderPeriodicity reminderPeriodicity
            join ProcessPeriodicity processPeriodicity on processPeriodicity.id = reminderPeriodicity.periodicityId
            where reminderPeriodicity.reminderId = :reminderId
            and reminderPeriodicity.status = :status
            """
    )
    Optional<List<ShortResponse>> findIdsByReminderIdAndStatus(Long reminderId, EntityStatus status);

    Optional<List<ReminderPeriodicity>> findByReminderIdAndStatus(Long reminderId, EntityStatus status);
}
