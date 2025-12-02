package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunTasks;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BillingRunTasksRepository extends JpaRepository<BillingRunTasks,Long> {

    List<BillingRunTasks> findByBillingIdAndStatusIn(Long billingId,List<EntityStatus> statuses);
    Optional<BillingRunTasks> findByBillingIdAndTaskIdAndStatusIn(Long billingId,Long taskId,List<EntityStatus> statuses);

    List<BillingRunTasks> findByTaskIdNotInAndStatusIn(List<Long> taskId,List<EntityStatus> statuses);

    @Query("""
            select brt from BillingRunTasks brt
            where brt.taskId = :taskId
            and brt.billingId = :billingId
            and brt.status = 'ACTIVE'
            """)
    Optional<BillingRunTasks> findBillingTaskByTaskIdAndBillingId(Long taskId, Long billingId);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(br.id, br.billingNumber, brt.createDate) from BillingRunTasks brt
            join BillingRun br on br.id = brt.billingId
            where brt.status = 'ACTIVE'
            and brt.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedBillings(Long taskId);
}
