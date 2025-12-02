package bg.energo.phoenix.repository.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineTask;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LatePaymentFineTaskRepository extends JpaRepository<LatePaymentFineTask, Long> {

    Optional<LatePaymentFineTask> findByTaskIdAndLatePaymentFineIdAndStatus(Long taskId, Long latePaymentFineId, ReceivableSubObjectStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(br.id, br.latePaymentNumber, brt.createDate) from LatePaymentFineTask brt
            join LatePaymentFine br on br.id = brt.latePaymentFineId
            where brt.status = 'ACTIVE'
            and brt.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedLatePayments(Long taskId);

}
