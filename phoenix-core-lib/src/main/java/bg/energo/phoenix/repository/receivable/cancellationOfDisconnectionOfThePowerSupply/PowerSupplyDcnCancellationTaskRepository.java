package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PowerSupplyDcnCancellationTaskRepository extends JpaRepository<PowerSupplyDcnCancellationTask, Long> {
    List<PowerSupplyDcnCancellationTask> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<EntityStatus> status);

    List<PowerSupplyDcnCancellationTask> findByPowerSupplyDcnCancellationIdAndTaskIdInAndStatusIn (Long cancellationId, Collection<Long> taskId, Collection<EntityStatus> status);

    List<PowerSupplyDcnCancellationTask> findByPowerSupplyDcnCancellationIdAndStatusIn(Long reconnectionId, Collection<EntityStatus> status);
    Optional<PowerSupplyDcnCancellationTask> findByIdAndPowerSupplyDcnCancellationIdAndStatus(Long taskId, Long cancellationId, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(dpsr.id, dpsr.number, dpsrt.createDate) from PowerSupplyDcnCancellationTask dpsrt
            join CancellationOfDisconnectionOfThePowerSupply dpsr on dpsr.id = dpsrt.powerSupplyDcnCancellationId
            where dpsrt.status = 'ACTIVE'
            and dpsrt.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedCancellations(Long taskId);



}
