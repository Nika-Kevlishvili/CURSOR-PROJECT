package bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderTasks;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PowerSupplyDisconnectionReminderTasksRepository extends JpaRepository<PowerSupplyDisconnectionReminderTasks,Long> {
    List<PowerSupplyDisconnectionReminderTasks> findByReminderIdAndTaskIdInAndStatusIn(Long reminderId, Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);
    List<PowerSupplyDisconnectionReminderTasks> findByReminderIdAndStatusIn(Long reminderId, Collection<ReceivableSubObjectStatus> status);
    List<PowerSupplyDisconnectionReminderTasks> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(r.id,r.reminderNumber, r.createDate) from PowerSupplyDisconnectionReminderTasks rt
            join PowerSupplyDisconnectionReminder r on r.id = rt.reminderId
            where rt.status = 'ACTIVE'
            and rt.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedToPowerSupplyDisconnectionReminder(Long taskId);

    Optional<PowerSupplyDisconnectionReminderTasks> findByTaskIdAndReminderIdAndStatus(Long taskId, Long reminderId, ReceivableSubObjectStatus status);
}
