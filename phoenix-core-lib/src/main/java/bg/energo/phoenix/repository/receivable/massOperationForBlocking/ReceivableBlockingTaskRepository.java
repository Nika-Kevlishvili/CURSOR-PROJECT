package bg.energo.phoenix.repository.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingTask;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReceivableBlockingTaskRepository extends JpaRepository<ReceivableBlockingTask, Long> {

    @Query("""
            select brt from ReceivableBlockingTask brt
            where brt.taskId = :taskId
            and brt.receivableBlockingId = :receivableBlockingId
            and brt.status = 'ACTIVE'
            """)
    Optional<ReceivableBlockingTask> findReceivableBlockingTaskByTaskIdAndReceivableBlockingId(Long taskId, Long receivableBlockingId);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(br.id, br.name, brt.createDate) from ReceivableBlockingTask brt
            join ReceivableBlocking br on br.id = brt.receivableBlockingId
            where brt.status = 'ACTIVE'
            and brt.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedReceivableBlocking(Long taskId);

    List<ReceivableBlockingTask> findByReceivableBlockingIdAndStatusIn(Long receivableBlockingId, Collection<ReceivableSubObjectStatus> status);

    List<ReceivableBlockingTask> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);

    List<ReceivableBlockingTask> findByReceivableBlockingIdAndTaskIdInAndStatusIn(Long receivableBlockingId, Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);

}
