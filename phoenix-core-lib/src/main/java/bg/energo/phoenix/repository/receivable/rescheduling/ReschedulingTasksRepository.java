package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingTasks;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReschedulingTasksRepository extends JpaRepository<ReschedulingTasks, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(r.id,r.reschedulingNumber, r.createDate) from ReschedulingTasks rt
            join Rescheduling r on r.id = rt.reschedulingId
            where rt.status = 'ACTIVE'
            and rt.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedToRescheduling(Long taskId);

    Optional<ReschedulingTasks> findByTaskIdAndReschedulingIdAndStatus(Long taskId, Long reschedulingId, ReceivableSubObjectStatus status);

}
