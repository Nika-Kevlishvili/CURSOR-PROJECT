package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyTasks;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconnectionOfThePowerSupplyTasksRepository extends JpaRepository<ReconnectionOfThePowerSupplyTasks, Long> {
    List<ReconnectionOfThePowerSupplyTasks> findByReconnectionIdAndTaskIdInAndStatusIn(Long reconnectionId, Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);
    List<ReconnectionOfThePowerSupplyTasks> findByReconnectionIdAndStatusIn(Long reconnectionId, Collection<ReceivableSubObjectStatus> status);
    List<ReconnectionOfThePowerSupplyTasks> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);
    Optional<ReconnectionOfThePowerSupplyTasks> findByTaskIdAndReconnectionIdAndStatus(Long taskId, Long powerSupplyRequestId, ReceivableSubObjectStatus status);
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(dpsr.id, dpsr.reconnectionNumber, dpsrt.createDate) from ReconnectionOfThePowerSupplyTasks dpsrt
            join ReconnectionOfThePowerSupply dpsr on dpsr.id = dpsrt.reconnectionId
            where dpsrt.status = 'ACTIVE'
            and dpsrt.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedReconnectionPowerSupplys(Long taskId);
}
